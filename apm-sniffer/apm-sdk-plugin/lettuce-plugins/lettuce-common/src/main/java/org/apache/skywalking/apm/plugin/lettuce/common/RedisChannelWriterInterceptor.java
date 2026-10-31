/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.lettuce.common;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.DecoratedCommand;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
import org.apache.skywalking.apm.agent.core.conf.Constants;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class RedisChannelWriterInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String PASSWORD_MASK = "******";
    private static final String ABBR = "...";
    private static final String AUTH = "AUTH";
    private static final StringCodec STRING_CODEC = new StringCodec();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) {
        String peer = (String) objInst.getSkyWalkingDynamicField();
        RedisCommand<?, ?, ?> writtenCommand = getWrittenCommand(allArguments[0]);
        RedisCommand<?, ?, ?> spanCarrierCommand = getSpanCarrierCommand(writtenCommand);
        if (spanCarrierCommand == null) {
            return;
        }
        // A command that is already done, such as one cancelled or timed out while its write was delayed, is dropped
        // by the CommandHandler, or gets no terminal call that reaches the span carrier, so its span would never be
        // finished.
        if (isDone(writtenCommand, spanCarrierCommand)) {
            return;
        }
        EnhancedInstance enhancedCommand = (EnhancedInstance) spanCarrierCommand;

        RedisCommandEnhanceInfo redisCommandEnhanceInfo = (RedisCommandEnhanceInfo) enhancedCommand.getSkyWalkingDynamicField();
        
        if (redisCommandEnhanceInfo == null) {
            redisCommandEnhanceInfo = new RedisCommandEnhanceInfo();
        }

        // command has been handle by another channel writer (cluster or sentinel case)
        AbstractSpan existingSpan = redisCommandEnhanceInfo.getSpan();
        if (existingSpan != null) {
            //set peer in last channel writer (delegate)
            if (peer != null) {
                existingSpan.setPeer(peer);
            }
            return;
        }

        String operationName = "Lettuce/";
        String key = Constants.EMPTY_STRING;
        String command = Constants.EMPTY_STRING;
        if (allArguments[0] instanceof RedisCommand) {
            RedisCommand<?, ?, ?> redisCommand = (RedisCommand<?, ?, ?>) allArguments[0];
            command = getCommandName(redisCommand.getType());
            operationName = operationName + command;
            if (LettucePluginConfig.Plugin.Lettuce.TRACE_REDIS_PARAMETERS) {
                key = getArgsKey(redisCommand);
            }
        } else if (allArguments[0] instanceof Collection) {
            operationName = operationName + "BATCH_WRITE";
            command = "BATCH_WRITE";
        }

        if (redisCommandEnhanceInfo.getSnapshot() != null) {
            AbstractSpan localSpan = ContextManager.createLocalSpan("RedisReactive/local");
            localSpan.setComponent(ComponentsDefine.LETTUCE);
            SpanLayer.asCache(localSpan);
            ContextManager.continued(redisCommandEnhanceInfo.getSnapshot());
        }
        
        AbstractSpan span = ContextManager.createExitSpan(operationName, peer);
        
        span.setComponent(ComponentsDefine.LETTUCE);
        Tags.CACHE_TYPE.set(span, "Redis");
        if (StringUtil.isNotEmpty(key)) {
            Tags.CACHE_KEY.set(span, key);
        }
        Tags.CACHE_CMD.set(span, command);
        parseOperation(command.toLowerCase()).ifPresent(op -> Tags.CACHE_OP.set(span, op));
        SpanLayer.asCache(span);
        span.prepareForAsync();
        ContextManager.stopSpan();

        if (redisCommandEnhanceInfo.getSnapshot() != null) {
            ContextManager.stopSpan();
        }
        
        enhancedCommand.setSkyWalkingDynamicField(redisCommandEnhanceInfo.setSpan(span));

        // The command may have been cancelled or timed out before the span was set, when its terminal interceptor
        // had no span to finish.
        if (isDone(writtenCommand, spanCarrierCommand)) {
            AbstractSpan unfinishedSpan = redisCommandEnhanceInfo.takeSpan();
            if (unfinishedSpan != null) {
                enhancedCommand.setSkyWalkingDynamicField(null);
                unfinishedSpan.errorOccurred();
                unfinishedSpan.asyncFinish();
            }
        }
    }

    private String getArgsKey(RedisCommand<?, ?, ?> redisCommand) {
        if (AUTH.equalsIgnoreCase(getCommandName(redisCommand.getType()))) {
            return PASSWORD_MASK;
        }
        CommandArgs<?, ?> args = redisCommand.getArgs();
        if (args == null) {
            return Constants.EMPTY_STRING;
        }
        ByteBuffer firstEncodedKey = args.getFirstEncodedKey();
        if (firstEncodedKey == null) {
            return Constants.EMPTY_STRING;
        }
        String key = STRING_CODEC.decodeKey(firstEncodedKey);
        if (StringUtil.isNotEmpty(key) && key.length() > LettucePluginConfig.Plugin.Lettuce.REDIS_PARAMETER_MAX_LENGTH) {
            key = StringUtil.cut(key, LettucePluginConfig.Plugin.Lettuce.REDIS_PARAMETER_MAX_LENGTH) + ABBR;
        }
        return key;
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        RedisCommand<?, ?, ?> redisCommand = getSpanCarrierCommand(allArguments[0]);
        if (redisCommand instanceof EnhancedInstance) {
            EnhancedInstance enhancedRedisCommand = (EnhancedInstance) redisCommand;
            AbstractSpan abstractSpan = RedisCommandEnhanceInfo.takeSpan(enhancedRedisCommand);
            if (abstractSpan != null) {
                enhancedRedisCommand.setSkyWalkingDynamicField(null);
                abstractSpan.log(t);
                abstractSpan.asyncFinish();
            }
        }
    }

    protected String getCommandName(ProtocolKeyword protocol) {
        return "UNKNOWN";
    }

    /**
     * @return the written command. For a batch write, it is the last command that is not done, which carries the span
     * of the whole batch, because a done command is dropped while the others are still sent. A command of the batch
     * that is cancelled while the batch is being written can still leave it without a span, or with two.
     */
    private static RedisCommand<?, ?, ?> getWrittenCommand(Object o) {
        if (o instanceof RedisCommand) {
            return (RedisCommand<?, ?, ?>) o;
        }
        if (!(o instanceof Collection)) {
            return null;
        }
        RedisCommand<?, ?, ?> lastCommand = null;
        RedisCommand<?, ?, ?> lastCommandNotDone = null;
        boolean anyCommandNotDone = false;
        for (RedisCommand<?, ?, ?> redisCommand : (Collection<RedisCommand<?, ?, ?>>) o) {
            lastCommand = redisCommand;
            if (redisCommand == null || !isDone(redisCommand, getSpanCarrierCommand(redisCommand))) {
                lastCommandNotDone = redisCommand;
                anyCommandNotDone = true;
            }
        }
        return anyCommandNotDone ? lastCommandNotDone : lastCommand;
    }

    /**
     * The CommandHandler checks the written command, while a wrapper of the span carrier, such as an AsyncCommand
     * whose cancellation cancels the carrier first, or a ClusterCommand, can look not done after the carrier is.
     */
    private static boolean isDone(RedisCommand<?, ?, ?> writtenCommand, RedisCommand<?, ?, ?> spanCarrierCommand) {
        return writtenCommand.isDone() || spanCarrierCommand != null && spanCarrierCommand.isDone();
    }

    static RedisCommand<?, ?, ?> getSpanCarrierCommand(Object o) {
        RedisCommand<?, ?, ?> command = getWrittenCommand(o);
        if (command instanceof DecoratedCommand) {
            while (command instanceof DecoratedCommand) {
                DecoratedCommand<?, ?, ?> wrapper = (DecoratedCommand<?, ?, ?>) command;
                command = wrapper.getDelegate();
            }
        }
        return command;
    }

    private Optional<String> parseOperation(String cmd) {
        if (LettucePluginConfig.Plugin.Lettuce.OPERATION_MAPPING_READ.contains(cmd)) {
            return Optional.of("read");
        }
        if (LettucePluginConfig.Plugin.Lettuce.OPERATION_MAPPING_WRITE.contains(cmd)) {
            return Optional.of("write");
        }
        return Optional.empty();
    }
}
