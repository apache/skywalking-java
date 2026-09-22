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

package org.apache.skywalking.apm.plugin.spring.webflux.v5.webclient;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.springframework.http.client.reactive.ClientHttpRequest;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

public class BodyInserterRequestInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        ClientHttpRequest clientHttpRequest = (ClientHttpRequest) allArguments[0];
        ContextCarrier contextCarrier = (ContextCarrier) objInst.getSkyWalkingDynamicField();
        if (contextCarrier != null) {
            inject(clientHttpRequest, contextCarrier);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        // Connectors like JdkClientHttpConnector invoke writeTo eagerly at assembly time,
        // before the exchange interceptor sets the carrier at subscription time. Retry the
        // injection when the returned Mono is subscribed, before the request is committed.
        if (objInst.getSkyWalkingDynamicField() != null || !(ret instanceof Mono)) {
            return ret;
        }
        final ClientHttpRequest clientHttpRequest = (ClientHttpRequest) allArguments[0];
        return Mono.defer(() -> {
            ContextCarrier contextCarrier = (ContextCarrier) objInst.getSkyWalkingDynamicField();
            if (contextCarrier != null) {
                try {
                    inject(clientHttpRequest, contextCarrier);
                } catch (Throwable t) {
                    // headers are read-only once the request is committed (e.g. re-subscribed by a retry)
                }
            }
            return (Mono<?>) ret;
        });
    }

    private void inject(ClientHttpRequest clientHttpRequest, ContextCarrier contextCarrier) {
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            clientHttpRequest.getHeaders().set(next.getHeadKey(), next.getHeadValue());
        }
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }
}
