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

package org.apache.skywalking.apm.plugin.redisson.v3;

import io.netty.channel.Channel;

import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.redisson.v3.util.ClassUtil;
import org.apache.skywalking.apm.util.StringUtil;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisConnection;
import org.redisson.client.protocol.CommandData;
import org.redisson.client.protocol.CommandsData;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Optional;

public class RedisConnectionMethodInterceptor implements InstanceMethodsAroundInterceptorV2, InstanceConstructorInterceptor {

    private static final ILog LOGGER = LogManager.getLogger(RedisConnectionMethodInterceptor.class);

    private static final String ABBR = "...";
    private static final String QUESTION_MARK = "?";
    private static final String DELIMITER_SPACE = " ";
    public static final Object STOP_SPAN_FLAG = new Object();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInvocationContext context) throws Throwable {
        String peer = (String) objInst.getSkyWalkingDynamicField();

        RedisConnection connection = (RedisConnection) objInst;
        Channel channel = connection.getChannel();
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        String dbInstance = remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();

        String operationName = "Redisson/";
        String command = "";
        Object[] arguments = new Object[0];

        if (allArguments[0] instanceof CommandsData) {
            operationName = operationName + "BATCH_EXECUTE";
            command = "BATCH_EXECUTE";
            if (RedissonPluginConfig.Plugin.Redisson.SHOW_BATCH_COMMANDS) {
                command += ":" + showBatchCommands((CommandsData) allArguments[0]);
            }
        } else if (allArguments[0] instanceof CommandData) {
            CommandData commandData = (CommandData) allArguments[0];
            command = commandData.getCommand().getName();
            if ("PING".equals(command) && !RedissonPluginConfig.Plugin.Redisson.SHOW_PING_COMMAND) {
                return;
            } else {
                operationName = operationName + command;
                arguments = commandData.getParams();
            }
        }

        AbstractSpan span = ContextManager.createExitSpan(operationName, peer);
        context.setContext(STOP_SPAN_FLAG);
        span.setComponent(ComponentsDefine.REDISSON);
        Tags.CACHE_TYPE.set(span, "Redis");
        Tags.CACHE_INSTANCE.set(span, dbInstance);
        Tags.CACHE_CMD.set(span, command);

        getKey(arguments).ifPresent(key -> Tags.CACHE_KEY.set(span, key));
        parseOperation(command.toLowerCase()).ifPresent(op -> Tags.CACHE_OP.set(span, op));
        SpanLayer.asCache(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) throws Throwable {
        if (Objects.nonNull(context.getContext())) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        if (Objects.nonNull(context.getContext())) {
            AbstractSpan span = ContextManager.activeSpan();
            span.log(t);
        }
    }

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        String peer = (String) ((EnhancedInstance) allArguments[0]).getSkyWalkingDynamicField();
        if (peer == null) {
            try {
                /*
                  In some high versions of redisson, such as 3.11.1.
                  The attribute address in the RedisClientConfig class changed from a lower version of the URI to a RedisURI.
                  But they all have the host and port attributes, so use the following code for compatibility.
                 */
                Object address = ClassUtil.getObjectField(((RedisClient) allArguments[0]).getConfig(), "address");
                String host = (String) ClassUtil.getObjectField(address, "host");
                String port = String.valueOf(ClassUtil.getObjectField(address, "port"));
                peer = host + ":" + port;
            } catch (Exception e) {
                LOGGER.warn("RedisConnection create peer error: ", e);
            }
        }
        objInst.setSkyWalkingDynamicField(peer);
    }

    private Optional<String> getKey(Object[] allArguments) {
        if (!RedissonPluginConfig.Plugin.Redisson.TRACE_REDIS_PARAMETERS) {
            return Optional.empty();
        }
        if (allArguments.length == 0) {
            return Optional.empty();
        }
        Object argument = allArguments[0];
        // include null
        if (!(argument instanceof String)) {
            return Optional.empty();
        }
        return Optional.of(StringUtil.cut((String) argument, RedissonPluginConfig.Plugin.Redisson.REDIS_PARAMETER_MAX_LENGTH));
    }

    private Optional<String> parseOperation(String cmd) {
        if (RedissonPluginConfig.Plugin.Redisson.OPERATION_MAPPING_READ.contains(cmd)) {
            return Optional.of("read");
        }
        if (RedissonPluginConfig.Plugin.Redisson.OPERATION_MAPPING_WRITE.contains(cmd)) {
            return Optional.of("write");
        }
        return Optional.empty();
    }

    private String showBatchCommands(CommandsData commandsData) {
        return commandsData.getCommands()
                           .stream()
                           .map(data -> data.getCommand().getName())
                           .collect(Collectors.joining(";"));
    }
}
