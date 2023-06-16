/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v20x;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.plugin.spring.cloud.gateway.v20x.define.EnhanceCacheObject;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.SPRING_CLOUD_GATEWAY;

public class HttpClientRequestInterceptor implements InstanceMethodsAroundInterceptorV2 {

    @Override
    public void beforeMethod(final EnhancedInstance objInst,
                             final Method method,
                             final Object[] allArguments,
                             final Class<?>[] argumentsTypes,
                             final MethodInvocationContext context) throws Throwable {
        
        /*
          In this plug-in, the HttpClientFinalizerSendInterceptor depends on the NettyRoutingFilterInterceptor
          When the NettyRoutingFilterInterceptor is not executed, the HttpClientFinalizerSendInterceptor has no meaning to be executed independently
          and using ContextManager.activeSpan() method would cause NPE as active span does not exist.
         */
        if (!ContextManager.isActive()) {
            return;
        }

        AbstractSpan span = ContextManager.activeSpan();

        URL url = new URL((String) allArguments[1]);
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan abstractSpan = ContextManager.createExitSpan(
                "SpringCloudGateway/sendRequest", contextCarrier, getPeer(url));
        abstractSpan.prepareForAsync();
        Tags.URL.set(abstractSpan, String.valueOf(allArguments[1]));
        abstractSpan.setLayer(SpanLayer.HTTP);
        abstractSpan.setComponent(SPRING_CLOUD_GATEWAY);
        ContextManager.stopSpan(abstractSpan);

        Function<? super HttpClientRequest, ? extends Publisher<Void>> handler = (Function<? super HttpClientRequest, ? extends Publisher<Void>>) allArguments[2];
        allArguments[2] = new Function<HttpClientRequest, Publisher<Void>>() {
            @Override
            public Publisher<Void> apply(final HttpClientRequest httpClientRequest) {
                //
                CarrierItem next = contextCarrier.items();
                if (httpClientRequest instanceof EnhancedInstance) {
                    ((EnhancedInstance) httpClientRequest).setSkyWalkingDynamicField(next);
                }
                return handler.apply(httpClientRequest);
            }
        };

        context.setContext(new EnhanceCacheObject(span, abstractSpan));
    }

    @Override
    public Object afterMethod(final EnhancedInstance objInst,
                              final Method method,
                              final Object[] allArguments,
                              final Class<?>[] argumentsTypes,
                              final Object ret,
                              MethodInvocationContext context) {
        EnhanceCacheObject enhanceCacheObject = (EnhanceCacheObject) context.getContext();
        Mono<HttpClientResponse> responseMono = (Mono<HttpClientResponse>) ret;
        return responseMono.doAfterSuccessOrError(new BiConsumer<HttpClientResponse, Throwable>() {
            @Override
            public void accept(final HttpClientResponse httpClientResponse, final Throwable throwable) {
                doAfterSuccessOrError(httpClientResponse, throwable, enhanceCacheObject);
            }
        });
    }

    void doAfterSuccessOrError(HttpClientResponse httpClientResponse, Throwable throwable, EnhanceCacheObject enhanceCacheObject) {
        try {
            //When executing the beforeMethod method, if the ContextManager is inactive, the enhanceCacheObject will be null.
            if (enhanceCacheObject == null) {
                return;
            }

            //The doAfterSuccessOrError method may be executed multiple times.
            if (enhanceCacheObject.isSpanFinish()) {
                return;
            }

            AbstractSpan abstractSpan = enhanceCacheObject.getSendSpan();
            if (throwable != null) {
                abstractSpan.log(throwable);
            } else if (httpClientResponse.status().code() > 400) {
                abstractSpan.errorOccurred();
            }
            Tags.HTTP_RESPONSE_STATUS_CODE.set(abstractSpan, httpClientResponse.status().code());

            abstractSpan.asyncFinish();
            enhanceCacheObject.getFilterSpan().asyncFinish();

            enhanceCacheObject.setSpanFinish(true);
        } catch (Throwable e) {
            //Catch unknown exceptions to avoid interrupting business processes.
        }
    }

    private String getPeer(URL url) {
        return url.getHost() + ":" + url.getPort();
    }

    @Override
    public void handleMethodException(final EnhancedInstance objInst,
                                      final Method method,
                                      final Object[] allArguments,
                                      final Class<?>[] argumentsTypes,
                                      final Throwable t,
                                      MethodInvocationContext context) {

    }
}
