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
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

public class DispatchedTaskRunInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) {
        if (objInst.getSkyWalkingDynamicField() instanceof CoroutineContext) {
            CoroutineContext context = (CoroutineContext) objInst.getSkyWalkingDynamicField();

            if (ContextManager.isActive() && context.getSnapshot().isFromCurrent()) {
                // Thread not switched, skip restore snapshot.
                return;
            }

            // Create local coroutine span
            AbstractSpan span = ContextManager.createLocalSpan(TracingRunnable.COROUTINE);
            span.setComponent(ComponentsDefine.KT_COROUTINE);
            span.tag(Tags.ofKey("continuation"), objInst.toString());
            // Recover with snapshot
            ContextManager.continued(context.getSnapshot());

            context.getSpans().push(span);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) {
        if (ContextManager.isActive() && objInst.getSkyWalkingDynamicField() instanceof CoroutineContext) {
            CoroutineContext context = (CoroutineContext) objInst.getSkyWalkingDynamicField();

            if (context.getSpans().empty()) {
                objInst.setSkyWalkingDynamicField(null);
            }

            // Finish local coroutine span
            AbstractSpan span = context.getSpans().pop();
            ContextManager.stopSpan(span);

            if (context.getSpans().empty()) {
                objInst.setSkyWalkingDynamicField(null);
            }
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive() && objInst.getSkyWalkingDynamicField() instanceof CoroutineContext) {
            CoroutineContext context = (CoroutineContext) objInst.getSkyWalkingDynamicField();

            if (context.getSpans().empty()) {
                objInst.setSkyWalkingDynamicField(null);
            }

            // Finish local coroutine span
            AbstractSpan span = context.getSpans().pop();
            ContextManager.stopSpan(span);
            objInst.setSkyWalkingDynamicField(null);

            if (context.getSpans().empty()) {
                objInst.setSkyWalkingDynamicField(null);
            }
        }
    }
}
