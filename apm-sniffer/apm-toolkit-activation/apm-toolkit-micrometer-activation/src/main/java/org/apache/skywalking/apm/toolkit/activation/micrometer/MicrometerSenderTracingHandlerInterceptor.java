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

package org.apache.skywalking.apm.toolkit.activation.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.SenderContext;
import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingSenderTracingHandler;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * {@link MicrometerSenderTracingHandlerInterceptor} define how to enhance classes
 * {@link SkywalkingSenderTracingHandler}.
 */
public class MicrometerSenderTracingHandlerInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String methodName = method.getName();
        if ("onStart".equals(methodName)) {
            SenderContext<Object> context = (SenderContext<Object>) allArguments[0];
            final ContextCarrier contextCarrier = new ContextCarrier();
            AbstractSpan span = ContextManager.createExitSpan(
                getOperationName(context), contextCarrier, SpanHelper.tryToGetPeer(context.getRemoteServiceAddress(), context.getAllKeyValues()));
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                context.getSetter().set(context.getCarrier(), next.getHeadKey(), next.getHeadValue());
            }
            span.setComponent(ComponentsDefine.MICROMETER);
        } else if ("onStop".equals(methodName)) {
            SenderContext<Object> context = (SenderContext<Object>) allArguments[0];
            SpanLayer spanLayer = TaggingHelper.toLayer(context.getAllKeyValues());
            AbstractSpan abstractSpan = ContextManager.activeSpan();
            abstractSpan
                .setPeer(SpanHelper.tryToGetPeer(context.getRemoteServiceAddress(), context.getAllKeyValues()))
                .setOperationName(getOperationName(context));
            context.getAllKeyValues()
                   .forEach(keyValue -> abstractSpan.tag(Tags.ofKey(keyValue.getKey()), keyValue.getValue()));
            if (spanLayer != null) {
                abstractSpan.setLayer(spanLayer);
            }
            ContextManager.stopSpan();
        } else if ("onError".equals(methodName)) {
            Observation.Context context = (Observation.Context) allArguments[0];
            ContextManager.activeSpan().log(context.getError());
        }
    }

    private static String getOperationName(final SenderContext<Object> context) {
        return StringUtil.isBlank(
            context.getContextualName()) ? context.getName() : context.getContextualName();
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }

}
