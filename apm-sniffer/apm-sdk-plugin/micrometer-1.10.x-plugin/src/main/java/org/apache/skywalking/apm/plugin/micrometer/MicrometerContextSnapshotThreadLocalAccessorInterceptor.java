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

package org.apache.skywalking.apm.plugin.micrometer;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.meter.micrometer.observation.SkywalkingContextSnapshotThreadLocalAccessor;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * {@link MicrometerContextSnapshotThreadLocalAccessorInterceptor} define how to enhance classes
 * {@link SkywalkingContextSnapshotThreadLocalAccessor}.
 */
public class MicrometerContextSnapshotThreadLocalAccessorInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        String methodName = method.getName();
        if ("getValue".equals(methodName)) {
            return ContextManager.capture();
        } else if ("setValue".equals(methodName)) {
            ContextSnapshot context = (ContextSnapshot) allArguments[0];
            // TODO: I want to continue an existing span, what should be the name?
            AbstractSpan span = ContextManager.createLocalSpan("continued");
            span.setComponent(ComponentsDefine.MICROMETER);
            ContextManager.continued(context);
        } else if ("reset".equals(methodName)) {
            // TODO: Can't do much about resetting
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }

}
