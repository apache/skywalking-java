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
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
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
    private static final String BATCH_WRITE = "BATCH_WRITE";
    /**
     * The writer of a cluster connection, which splits a batch by the slots of its commands, and writes the commands of
     * each slot to the connection of its node, where they are traced as a batch of their own.
     */
    private static final String CLUSTER_DISTRIBUTION_CHANNEL_WRITER = "io.lettuce.core.cluster.ClusterDistributionChannelWriter";
    private static final StringCodec STRING_CODEC = new StringCodec();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) {
        String peer = (String) objInst.getSkyWalkingDynamicField();
        if (allArguments[0] instanceof RedisCommand) {
            beforeWrite(peer, (RedisCommand<?, ?, ?>) allArguments[0]);
        } else if (allArguments[0] instanceof Collection && !isClusterDistributionChannelWriter(objInst)) {
            beforeWrite(peer, (Collection<RedisCommand<?, ?, ?>>) allArguments[0]);
        }
    }

    private void beforeWrite(String peer, RedisCommand<?, ?, ?> command) {
        RedisCommand<?, ?, ?> spanCarrierCommand = getSpanCarrierCommand(command);
        if (!(spanCarrierCommand instanceof EnhancedInstance)) {
            return;
        }
        // command has been handle by another channel writer (cluster or sentinel case)
        RedisWriteSpan existingWriteSpan = RedisCommandEnhanceInfo.getWriteSpan((EnhancedInstance) spanCarrierCommand);
        if (existingWriteSpan != null) {
            //set peer in last channel writer (delegate)
            if (peer != null) {
                existingWriteSpan.setPeer(peer);
            }
            return;
        }
        // A command that is already done, such as one cancelled or timed out while its write was delayed, is dropped
        // by the CommandHandler, or gets no terminal call that reaches the span carrier, so its span would never be
        // finished.
        if (isDone(command, spanCarrierCommand)) {
            return;
        }

        String commandName = getCommandName(command.getType());
        String key = Constants.EMPTY_STRING;
        if (LettucePluginConfig.Plugin.Lettuce.TRACE_REDIS_PARAMETERS) {
            key = getArgsKey(command);
        }
        RedisWriteSpan writeSpan = createWriteSpan(peer, commandName, key, getSnapshot(spanCarrierCommand));
        if (!traceCommand(writeSpan, command, spanCarrierCommand)) {
            writeSpan.errorOccurred();
        }
        writeSpan.finishCommand();
    }

    /**
     * The commands of a batch are written to the channel and flushed together, so they share one span, which is
     * finished when the last of them is finished. A command that is already done is dropped by the CommandHandler, so
     * it has no share of the span.
     */
    private void beforeWrite(String peer, Collection<RedisCommand<?, ?, ?>> commands) {
        boolean traced = false;
        RedisCommand<?, ?, ?> lastCommandToTrace = null;
        for (RedisCommand<?, ?, ?> command : commands) {
            RedisCommand<?, ?, ?> spanCarrierCommand = getSpanCarrierCommand(command);
            if (!(spanCarrierCommand instanceof EnhancedInstance)) {
                continue;
            }
            RedisWriteSpan existingWriteSpan = RedisCommandEnhanceInfo.getWriteSpan((EnhancedInstance) spanCarrierCommand);
            if (existingWriteSpan != null) {
                // The batch has been traced by another channel writer, which delegates the write to this one.
                traced = true;
                if (peer != null) {
                    existingWriteSpan.setPeer(peer);
                }
            } else if (!isDone(command, spanCarrierCommand)) {
                lastCommandToTrace = command;
            }
        }
        if (traced || lastCommandToTrace == null) {
            return;
        }

        RedisWriteSpan writeSpan = createWriteSpan(
            peer, BATCH_WRITE, Constants.EMPTY_STRING, getSnapshot(getSpanCarrierCommand(lastCommandToTrace)));
        boolean anyCommandTraced = false;
        for (RedisCommand<?, ?, ?> command : commands) {
            RedisCommand<?, ?, ?> spanCarrierCommand = getSpanCarrierCommand(command);
            if (spanCarrierCommand instanceof EnhancedInstance && !isDone(command, spanCarrierCommand)
                && traceCommand(writeSpan, command, spanCarrierCommand)) {
                anyCommandTraced = true;
            }
        }
        // Every command of the batch was cancelled or timed out before it got its share of the span.
        if (!anyCommandTraced) {
            writeSpan.errorOccurred();
        }
        writeSpan.finishCommand();
    }

    /**
     * @return the exit span of a write, with the share of the writer, which keeps the span unfinished until every
     * command of the write has its share. The writer must finish its share at last.
     */
    private RedisWriteSpan createWriteSpan(String peer, String command, String key, ContextSnapshot snapshot) {
        if (snapshot != null) {
            AbstractSpan localSpan = ContextManager.createLocalSpan("RedisReactive/local");
            localSpan.setComponent(ComponentsDefine.LETTUCE);
            SpanLayer.asCache(localSpan);
            ContextManager.continued(snapshot);
        }
        
        AbstractSpan span = ContextManager.createExitSpan("Lettuce/" + command, peer);
        
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

        if (snapshot != null) {
            ContextManager.stopSpan();
        }
        return new RedisWriteSpan(span);
    }

    /**
     * Gives the command a share of the write span, which its terminal interceptor finishes.
     *
     * @return false if the command did not keep its share, because it already has a span, or it was cancelled or
     * timed out before it got the share, when its terminal interceptor had no span to finish.
     */
    private static boolean traceCommand(RedisWriteSpan writeSpan, RedisCommand<?, ?, ?> command,
                                        RedisCommand<?, ?, ?> spanCarrierCommand) {
        EnhancedInstance enhancedCommand = (EnhancedInstance) spanCarrierCommand;
        RedisCommandEnhanceInfo enhanceInfo = (RedisCommandEnhanceInfo) enhancedCommand.getSkyWalkingDynamicField();
        if (enhanceInfo == null) {
            enhanceInfo = new RedisCommandEnhanceInfo();
        } else if (enhanceInfo.getWriteSpan() != null) {
            // The same command is in the batch more than once.
            return false;
        }
        writeSpan.addCommand();
        enhancedCommand.setSkyWalkingDynamicField(enhanceInfo.setWriteSpan(writeSpan));

        if (isDone(command, spanCarrierCommand)) {
            RedisWriteSpan unfinishedWriteSpan = enhanceInfo.takeWriteSpan();
            if (unfinishedWriteSpan != null) {
                enhancedCommand.setSkyWalkingDynamicField(null);
                unfinishedWriteSpan.errorOccurred();
                unfinishedWriteSpan.finishCommand();
            }
            return false;
        }
        return true;
    }

    private static ContextSnapshot getSnapshot(RedisCommand<?, ?, ?> spanCarrierCommand) {
        RedisCommandEnhanceInfo enhanceInfo =
            (RedisCommandEnhanceInfo) ((EnhancedInstance) spanCarrierCommand).getSkyWalkingDynamicField();
        return enhanceInfo == null ? null : enhanceInfo.getSnapshot();
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
        if (allArguments[0] instanceof RedisCommand) {
            finishFailedWrite((RedisCommand<?, ?, ?>) allArguments[0], t);
        } else if (allArguments[0] instanceof Collection && !isClusterDistributionChannelWriter(objInst)) {
            for (RedisCommand<?, ?, ?> command : (Collection<RedisCommand<?, ?, ?>>) allArguments[0]) {
                finishFailedWrite(command, t);
            }
        }
    }

    private static void finishFailedWrite(RedisCommand<?, ?, ?> command, Throwable t) {
        RedisCommand<?, ?, ?> spanCarrierCommand = getSpanCarrierCommand(command);
        if (spanCarrierCommand instanceof EnhancedInstance) {
            EnhancedInstance enhancedRedisCommand = (EnhancedInstance) spanCarrierCommand;
            RedisWriteSpan writeSpan = RedisCommandEnhanceInfo.takeWriteSpan(enhancedRedisCommand);
            if (writeSpan != null) {
                enhancedRedisCommand.setSkyWalkingDynamicField(null);
                writeSpan.log(t);
                writeSpan.finishCommand();
            }
        }
    }

    protected String getCommandName(ProtocolKeyword protocol) {
        return "UNKNOWN";
    }

    private static boolean isClusterDistributionChannelWriter(EnhancedInstance channelWriter) {
        return CLUSTER_DISTRIBUTION_CHANNEL_WRITER.equals(channelWriter.getClass().getName());
    }

    /**
     * The CommandHandler checks the written command, while a wrapper of the span carrier, such as an AsyncCommand
     * whose cancellation cancels the carrier first, or a ClusterCommand, can look not done after the carrier is.
     */
    private static boolean isDone(RedisCommand<?, ?, ?> writtenCommand, RedisCommand<?, ?, ?> spanCarrierCommand) {
        return writtenCommand.isDone() || spanCarrierCommand.isDone();
    }

    /**
     * @return the command that carries the span of a written command, which is the command decorated by it, if any.
     */
    static RedisCommand<?, ?, ?> getSpanCarrierCommand(Object o) {
        if (!(o instanceof RedisCommand)) {
            return null;
        }
        RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) o;
        while (command instanceof DecoratedCommand) {
            DecoratedCommand<?, ?, ?> wrapper = (DecoratedCommand<?, ?, ?>) command;
            command = wrapper.getDelegate();
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
