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
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

public class ThreadPerTaskExecutorRunInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Object skyWalkingDynamicField = objInst.getSkyWalkingDynamicField();
        if (skyWalkingDynamicField == null) {
            return;
        }

        if (!(skyWalkingDynamicField instanceof ContextSnapshot)) {
            return;
        }

        AbstractSpan span = ContextManager.createLocalSpan(getOperationName(objInst, method));
        span.setComponent(ComponentsDefine.THREAD_PER_TASK_EXECUTOR);
        setCarrierThread(span);

        ContextSnapshot contextSnapshot = (ContextSnapshot) skyWalkingDynamicField;
        ContextManager.continued(contextSnapshot);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }

    private String getOperationName(EnhancedInstance objInst, Method method) {
        return objInst.getClass().getName() + "." + method.getName();
    }

    private void setCarrierThread(AbstractSpan span) {
        String threadInfo = Thread.currentThread().toString();
        if (threadInfo.startsWith("VirtualThread")) {
            String[] parts = threadInfo.split("@");
            if (parts.length >= 1) {
                Tags.CARRIER_THREAD.set(span, parts[1]);
            }
        }
    }
}
