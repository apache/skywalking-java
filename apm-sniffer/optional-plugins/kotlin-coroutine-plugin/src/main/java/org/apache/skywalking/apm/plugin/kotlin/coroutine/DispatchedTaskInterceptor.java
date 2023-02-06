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

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DispatchedTaskInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) {
        ContextSnapshot snapshot = (ContextSnapshot) objInst.getSkyWalkingDynamicField();
        if (snapshot != null) {
            if (ContextManager.isActive() && snapshot.isFromCurrent()) {
                // Thread not switched, skip restore snapshot.
                return;
            }

            // Create local coroutine span
            AbstractSpan span = ContextManager.createLocalSpan(TracingRunnable.COROUTINE);
            span.setComponent(ComponentsDefine.KT_COROUTINE);

            String[] elements = Utils.getStackTraceElements((Runnable) objInst);
            Map<String, String> eventMap = new HashMap<String, String>();
            for (int i = 0; i < elements.length; i++) {
                eventMap.put("coroutine.stack[" + i + "]", elements[i]);
            }
            span.log(System.currentTimeMillis(), eventMap);

            // Recover with snapshot
            ContextManager.continued(snapshot);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) {
        if (ContextManager.isActive() && ContextManager.activeSpan() != null) {
            ContextManager.stopSpan(ContextManager.activeSpan());
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive() && ContextManager.activeSpan() != null) {
            ContextManager.stopSpan(ContextManager.activeSpan());
        }
    }
}
