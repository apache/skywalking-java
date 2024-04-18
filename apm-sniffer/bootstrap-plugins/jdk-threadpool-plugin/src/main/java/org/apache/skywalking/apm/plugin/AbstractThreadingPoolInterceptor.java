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

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import java.lang.reflect.Method;

public abstract class AbstractThreadingPoolInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        if (notToEnhance(allArguments)) {
            return;
        }

        Object wrappedObject = wrap(allArguments[0]);
        if (wrappedObject != null) {
            allArguments[0] = wrappedObject;
        }
    }

    /**
     * wrap the Callable or Runnable object if needed
     * @param param  Callable or Runnable object
     * @return Wrapped object or null if not needed to wrap
     */
    public abstract Object wrap(Object param);

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        if (notToEnhance(allArguments)) {
            return;
        }

        ContextManager.activeSpan().log(t);
    }

    private boolean notToEnhance(Object[] allArguments) {
        if (!ContextManager.isActive()) {
            return true;
        }

        if (allArguments == null || allArguments.length < 1) {
            return true;
        }

        Object argument = allArguments[0];

        // Avoid duplicate enhancement, such as the case where it has already been enhanced by RunnableWrapper or CallableWrapper with toolkit.
        return argument instanceof EnhancedInstance && ((EnhancedInstance) argument).getSkyWalkingDynamicField() instanceof ContextSnapshot;
    }
}
