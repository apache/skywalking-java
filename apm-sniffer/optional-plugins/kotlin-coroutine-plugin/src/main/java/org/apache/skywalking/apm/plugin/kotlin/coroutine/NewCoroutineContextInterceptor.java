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

package org.apache.skywalking.apm.plugin.kotlin.coroutine;

import kotlin.coroutines.CoroutineContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.StaticMethodsAroundInterceptorV2;

import java.lang.reflect.Method;

public class NewCoroutineContextInterceptor implements StaticMethodsAroundInterceptorV2 {

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, MethodInvocationContext context) {

    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Object ret, MethodInvocationContext context) {
        if (ContextManager.isActive()) {
            // Create TracingCoroutineContext when have been in tracing context already.
            // Kotlin coroutine plugin does not automatically activate tracing.
            CoroutineContext coroutineContext = (CoroutineContext) ret;
            // Provide context snapshot for current tracing to TracingCoroutineContext.
            return coroutineContext.plus(new TracingCoroutineContext(ContextManager.capture()));
        }

        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Throwable t, MethodInvocationContext context) {
    }
}
