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

package org.apache.skywalking.apm.plugin.xmemcached.v2;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * {@link XMemcachedMethodInterceptor} intercept the operation method, record the memcached host, operation name and the
 * key of the operation.
 */
public class XMemcachedMethodInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String XMEMCACHED = "XMemcached/";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        AbstractSpan span = ContextManager.createExitSpan(XMEMCACHED + method.getName(), peer);
        span.setComponent(ComponentsDefine.XMEMCACHED);
        Tags.CACHE_TYPE.set(span, ComponentsDefine.XMEMCACHED.getName());
        Tags.CACHE_CMD.set(span, method.getName());
        Tags.CACHE_KEY.set(span, allArguments[0].toString());
        SpanLayer.asCache(span);
        String methodName = method.getName();
        parseOperation(methodName).ifPresent(op -> Tags.CACHE_OP.set(span, op));
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
        if (MemcachedPluginConfig.Plugin.Memcached.OPERATION_MAPPING_READ.contains(cmd)) {
            return Optional.of("read");
        }
        if (MemcachedPluginConfig.Plugin.Memcached.OPERATION_MAPPING_WRITE.contains(cmd)) {
            return Optional.of("write");
        }
        return Optional.empty();
    }
}
