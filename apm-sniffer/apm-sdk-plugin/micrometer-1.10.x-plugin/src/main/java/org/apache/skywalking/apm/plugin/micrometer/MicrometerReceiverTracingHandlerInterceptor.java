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

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.ReceiverContext;
import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.meter.micrometer.observation.SkywalkingReceiverTracingHandler;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * {@link MicrometerReceiverTracingHandlerInterceptor} define how to enhance classes
 * {@link SkywalkingReceiverTracingHandler}.
 */
public class MicrometerReceiverTracingHandlerInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String methodName = method.getName();
        if ("onStart".equals(methodName)) {
            ReceiverContext<Object> context = (ReceiverContext<Object>) allArguments[0];
            final ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                next.setHeadValue(context.getGetter().get(context.getCarrier(), next.getHeadKey()));
            }
            AbstractSpan span = ContextManager.createEntrySpan(context.getName(), contextCarrier);
            span.setComponent(ComponentsDefine.MICROMETER);
            //            TODO: We can't really set these because ReceiverContext is super generic. We don't know what the protocol will be. We can guess it from the tags maybe? But there's no certainty that same tags will be used everywhere. OTOH
            //            entrySpan.setLayer(SpanLayer.HTTP);
            //            Tags.URL.set(entrySpan, httpRequest.path());
            //            Tags.HTTP.METHOD.set(entrySpan, httpRequest.method().name());

        } else if ("onStop".equals(methodName)) {
            ReceiverContext<Object> context = (ReceiverContext<Object>) allArguments[0];
            AbstractSpan abstractSpan = ContextManager.activeSpan();
            abstractSpan
                .setPeer(tryToGetPeer(context))
                .setOperationName(StringUtil.isBlank(
                    context.getContextualName()) ? context.getName() : context.getContextualName());
            context.getAllKeyValues()
                   .forEach(keyValue -> abstractSpan.tag(Tags.ofKey(keyValue.getKey()), keyValue.getValue()));
            ContextManager.stopSpan();
        } else if ("onError".equals(methodName)) {
            Observation.Context context = (Observation.Context) allArguments[0];
            ContextManager.activeSpan().log(context.getError());
        }
    }

    private String tryToGetPeer(ReceiverContext<Object> context) {
        if (context.getRemoteServiceAddress() != null) {
            return context.getRemoteServiceAddress();
        }
        KeyValue uri = context.getLowCardinalityKeyValue("uri");
        if (uri != null) {
            return uri.getValue();
        }
        return context.getAllKeyValues()
                      .stream()
                      .filter(keyValue -> "uri".equalsIgnoreCase(keyValue.getKey()) || "http.url".equalsIgnoreCase(
                          keyValue.getKey()))
                      .findFirst()
                      .map(KeyValue::getValue)
                      .orElse("unknown");
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
