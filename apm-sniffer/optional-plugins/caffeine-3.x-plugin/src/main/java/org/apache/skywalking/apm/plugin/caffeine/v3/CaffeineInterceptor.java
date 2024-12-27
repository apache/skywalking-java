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

package org.apache.skywalking.apm.plugin.caffeine.v3;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import static org.apache.skywalking.apm.plugin.caffeine.v3.CaffeineOperationTransform.transformOperation;

public class CaffeineInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(final EnhancedInstance objInst,
                             final Method method,
                             final Object[] allArguments,
                             final Class<?>[] argumentsTypes,
                             final MethodInterceptResult result) throws Throwable {
        String methodName = method.getName();
        AbstractSpan span = ContextManager.createLocalSpan("Caffeine/" + methodName);
        span.setComponent(ComponentsDefine.CAFFEINE);
        if (allArguments != null && allArguments.length > 0 && allArguments[0] != null) {
            if (allArguments[0] instanceof String) {
                Tags.CACHE_KEY.set(span, allArguments[0].toString());
            } else if (allArguments[0] instanceof Map) {
                String keys = ((Map<?, ?>) allArguments[0])
                    .keySet().stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
                Tags.CACHE_KEY.set(span, keys);
            } else if (allArguments[0] instanceof Set) {
                String keys = ((Set<?>) allArguments[0])
                    .stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
                Tags.CACHE_KEY.set(span, keys);
            }
        }
        Tags.CACHE_TYPE.set(span, ComponentsDefine.CAFFEINE.getName());
        Tags.CACHE_CMD.set(span, methodName);
        transformOperation(methodName).ifPresent(op -> Tags.CACHE_OP.set(span, op));
        SpanLayer.asCache(span);
    }

    @Override
    public Object afterMethod(final EnhancedInstance objInst,
                              final Method method,
                              final Object[] allArguments,
                              final Class<?>[] argumentsTypes,
                              final Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(final EnhancedInstance objInst,
                                      final Method method,
                                      final Object[] allArguments,
                                      final Class<?>[] argumentsTypes,
                                      final Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
