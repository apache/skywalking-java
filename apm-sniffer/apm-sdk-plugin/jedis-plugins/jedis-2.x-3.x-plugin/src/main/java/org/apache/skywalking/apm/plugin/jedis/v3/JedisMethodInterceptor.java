/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.jedis.v3;

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
import java.util.Optional;

public class JedisMethodInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        AbstractSpan span = ContextManager.createExitSpan("Jedis/" + method.getName(), peer);
        span.setComponent(ComponentsDefine.JEDIS);
        SpanLayer.asCache(span);
        String methodName = method.getName();
        Tags.CACHE_TYPE.set(span, "Redis");
        Tags.CACHE_CMD.set(span, methodName);
        getKey(allArguments).ifPresent(key -> Tags.CACHE_KEY.set(span, key));
        parseOperation(methodName).ifPresent(op -> Tags.CACHE_OP.set(span, op));
    }

    private Optional<String> getKey(Object[] allArguments) {
        if (!JedisPluginConfig.Plugin.Jedis.TRACE_REDIS_PARAMETERS) {
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
        return Optional.of(StringUtil.cut((String) argument, JedisPluginConfig.Plugin.Jedis.REDIS_PARAMETER_MAX_LENGTH));
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.log(t);
    }

    private Optional<String> parseOperation(String cmd) {
        if (JedisPluginConfig.Plugin.Jedis.OPERATION_MAPPING_READ.contains(cmd)) {
            return Optional.of("read");
        }
        if (JedisPluginConfig.Plugin.Jedis.OPERATION_MAPPING_WRITE.contains(cmd)) {
            return Optional.of("write");
        }
        return Optional.empty();
    }
}
