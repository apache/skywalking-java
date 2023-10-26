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

package org.apache.skywalking.apm.plugin.pulsar.common;

import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Record;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.util.Map;

public class FunctionApiInterceptor implements InstanceMethodsAroundInterceptorV2 {
    private static final String OPERATION_NAME = "Function/Process";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInvocationContext ctx) throws Throwable {
        Context context = (Context) allArguments[1];
        Record<?> record = context.getCurrentRecord();
        if (null == record) {
            return;
        }
        Map<String, String> properties = record.getProperties();
        if (null == properties || properties.isEmpty()) {
            return;
        }

        ContextCarrier carrier = new ContextCarrier();
        CarrierItem next = carrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(properties.get(next.getHeadKey()));
        }

        AbstractSpan span = ContextManager.createLocalSpan(OPERATION_NAME);
        ContextManager.extract(carrier);
        span.setComponent(ComponentsDefine.PULSAR_FUNCTION);
        ctx.setContext(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret, MethodInvocationContext ctx) throws Throwable {
        Object context = ctx.getContext();
        if (context instanceof AbstractSpan) {
            AbstractSpan span1 = (AbstractSpan) context;
            ContextManager.stopSpan(span1);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext ctx) {
        // ignore
    }
}
