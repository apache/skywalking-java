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

package org.apache.skywalking.apm.plugin.lettuce.v5;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.DecoratedCommand;
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
import java.util.List;
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
        RedisCommand<?, ?, ?> spanCarrierCommand = getSpanCarrierCommand(allArguments[0]);
        if (spanCarrierCommand == null) {
            return;
        }
        EnhancedInstance enhancedCommand = (EnhancedInstance) spanCarrierCommand;

        // command has been handle by another channel writer (cluster or sentinel case)
        if (enhancedCommand.getSkyWalkingDynamicField() != null) {
            //set peer in last channel writer (delegate)
            if (peer != null) {
                AbstractSpan span = (AbstractSpan) enhancedCommand.getSkyWalkingDynamicField();
                span.setPeer(peer);
            }
            return;
        }

        String operationName = "Lettuce/";
        String key = Constants.EMPTY_STRING;
        String command = Constants.EMPTY_STRING;
        if (allArguments[0] instanceof RedisCommand) {
            RedisCommand<?, ?, ?> redisCommand = (RedisCommand<?, ?, ?>) allArguments[0];
            command = redisCommand.getType().name();
            operationName = operationName + command;
            if (LettucePluginConfig.Plugin.Lettuce.TRACE_REDIS_PARAMETERS) {
                key = getArgsKey(redisCommand);
            }
        } else if (allArguments[0] instanceof Collection) {
            operationName = operationName + "BATCH_WRITE";
            command = "BATCH_WRITE";
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
        enhancedCommand.setSkyWalkingDynamicField(span);
    }

    private String getArgsKey(RedisCommand<?, ?, ?> redisCommand) {
        if (AUTH.equalsIgnoreCase(redisCommand.getType().name())) {
            return PASSWORD_MASK;
        }
        CommandArgs<?, ?> args = redisCommand.getArgs();
        if (args == null) {
            return Constants.EMPTY_STRING;
        } 
        ByteBuffer firstEncodedKey = args.getFirstEncodedKey();
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
        if (redisCommand instanceof EnhancedInstance && ((EnhancedInstance) redisCommand).getSkyWalkingDynamicField() != null) {
            EnhancedInstance enhancedRedisCommand = (EnhancedInstance) redisCommand;
            AbstractSpan abstractSpan = (AbstractSpan) enhancedRedisCommand.getSkyWalkingDynamicField();
            enhancedRedisCommand.setSkyWalkingDynamicField(null);
            abstractSpan.log(t);
            abstractSpan.asyncFinish();
        }
    }

    private static RedisCommand<?, ?, ?> getSpanCarrierCommand(Object o) {
        RedisCommand<?, ?, ?> command = null;
        if (o instanceof RedisCommand) {
            command = (RedisCommand<?, ?, ?>) o;
        } else if (o instanceof List) {
            List<?> list = (List<?>) o;
            command = list.isEmpty() ? null : (RedisCommand<?, ?, ?>) list.get(list.size() - 1);
        } else if (o instanceof Collection) {
            Collection<RedisCommand<?, ?, ?>> redisCommands = (Collection<RedisCommand<?, ?, ?>>) o;
            RedisCommand<?, ?, ?> last = null;
            for (RedisCommand<?, ?, ?> redisCommand : redisCommands) {
                last = redisCommand;
            }
            command = last;
        }
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
