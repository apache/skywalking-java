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

package org.apache.skywalking.apm.plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.wrapper.SwCallableWrapper;

public class ThreadPoolInvokeMethodInterceptor implements InstanceMethodsAroundInterceptorV2 {

    private static final String OPERATION_NAME_PREFIX = "ThreadPoolExecutor/";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInvocationContext context) throws Throwable {
        if (!shouldEnhance(allArguments)) {
            return;
        }

        AbstractSpan span = ContextManager.createLocalSpan(OPERATION_NAME_PREFIX + method.getName());
        span.setComponent(ComponentsDefine.JDK_THREADING);
        context.setContext(span);

        ContextSnapshot contextSnapshot = ContextManager.capture();
        Collection<?> callables = (Collection<?>) allArguments[0];
        List<Object> wrappedCallables = new ArrayList<>(callables.size());
        for (Object callable : callables) {
            wrappedCallables.add(wrap(callable, contextSnapshot));
        }
        allArguments[0] = wrappedCallables;
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret, MethodInvocationContext context) throws Throwable {
        AbstractSpan span = (AbstractSpan) context.getContext();
        if (span != null) {
            ContextManager.stopSpan(span);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        AbstractSpan span = (AbstractSpan) context.getContext();
        if (span != null) {
            span.log(t);
        }
    }

    private boolean shouldEnhance(Object[] allArguments) {
        return ContextManager.isActive()
            && allArguments != null
            && allArguments.length > 0
            && allArguments[0] instanceof Collection;
    }

    private Object wrap(Object callable, ContextSnapshot contextSnapshot) {
        if (!(callable instanceof Callable) || callable instanceof SwCallableWrapper || hasCapturedContext(callable)) {
            return callable;
        }
        return new SwCallableWrapper((Callable) callable, contextSnapshot);
    }

    private boolean hasCapturedContext(Object callable) {
        return callable instanceof EnhancedInstance
            && ((EnhancedInstance) callable).getSkyWalkingDynamicField() instanceof ContextSnapshot;
    }
}
