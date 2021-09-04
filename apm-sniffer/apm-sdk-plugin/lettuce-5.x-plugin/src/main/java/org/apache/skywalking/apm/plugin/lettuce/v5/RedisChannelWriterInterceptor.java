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
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
public class RedisChannelWriterInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String PASSWORD_MASK = "******";
    private static final String ABBR = "...";
    private static final String DELIMITER_SPACE = " ";
    private static final String AUTH = "AUTH";

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

        StringBuilder dbStatement = new StringBuilder();
        String operationName = "Lettuce/";
        if (allArguments[0] instanceof RedisCommand) {
            RedisCommand<?, ?, ?> redisCommand = (RedisCommand<?, ?, ?>) allArguments[0];
            String command = redisCommand.getType().name();
            operationName = operationName + command;
            dbStatement.append(command);
            if (LettucePluginConfig.Plugin.Lettuce.TRACE_REDIS_PARAMETERS) {
                dbStatement.append(DELIMITER_SPACE).append(getArgsStatement(redisCommand));
            }
        } else if (allArguments[0] instanceof Collection) {
            Collection<RedisCommand<?, ?, ?>> redisCommands = (Collection<RedisCommand<?, ?, ?>>) allArguments[0];
            operationName = operationName + "BATCH_WRITE";
            for (RedisCommand<?, ?, ?> redisCommand : redisCommands) {
                dbStatement.append(redisCommand.getType().name()).append(";");
            }
        }
        AbstractSpan span = ContextManager.createExitSpan(operationName, peer);
        span.setComponent(ComponentsDefine.LETTUCE);
        Tags.DB_TYPE.set(span, "Redis");
        Tags.DB_STATEMENT.set(span, dbStatement.toString());
        SpanLayer.asCache(span);
        span.prepareForAsync();
        ContextManager.stopSpan();
        enhancedCommand.setSkyWalkingDynamicField(span);
    }

    private String getArgsStatement(RedisCommand<?, ?, ?> redisCommand) {
        String statement;
        if (AUTH.equalsIgnoreCase(redisCommand.getType().name())) {
            statement = PASSWORD_MASK;
        } else {
            CommandArgs<?, ?> args = redisCommand.getArgs();
            statement = (args != null) ? args.toCommandString() : Constants.EMPTY_STRING;
        }
        if (StringUtil.isNotEmpty(statement) && statement.length() > LettucePluginConfig.Plugin.Lettuce.REDIS_PARAMETER_MAX_LENGTH) {
            statement = statement.substring(0, LettucePluginConfig.Plugin.Lettuce.REDIS_PARAMETER_MAX_LENGTH) + ABBR;
        }
        return statement;
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
}
