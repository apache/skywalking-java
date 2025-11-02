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

import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpClientSendAsyncInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        HttpRequest request = (HttpRequest) allArguments[0];
        URI uri = request.uri();

        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(buildOperationName(request.method(), uri), contextCarrier, getPeer(uri));

        if (request instanceof EnhancedInstance) {
            ((EnhancedInstance) request).setSkyWalkingDynamicField(contextCarrier);
        }

        span.setComponent(ComponentsDefine.JDK_HTTP);
        Tags.HTTP.METHOD.set(span, request.method());
        Tags.URL.set(span, String.valueOf(uri));
        SpanLayer.asHttp(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {

        if (ContextManager.isActive()) {
            AbstractSpan span = ContextManager.activeSpan();
            span.prepareForAsync();

            if (ret != null) {
                CompletableFuture<?> future = (CompletableFuture<?>) ret;
                ret = future.whenComplete((response, throwable) -> {
                    try {
                        if (throwable != null) {
                            span.errorOccurred();
                            span.log(throwable);
                            return;
                        }
                        if (response instanceof HttpResponse) {
                            HttpResponse<?> httpResponse = (HttpResponse<?>) response;
                            int statusCode = httpResponse.statusCode();
                            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
                            if (statusCode >= 400) {
                                span.errorOccurred();
                            }
                        }
                    } finally {
                        span.asyncFinish();
                    }
                });
            } else {
                Map<String, String> eventMap = new HashMap<String, String>();
                eventMap.put("error", "No response");
                span.log(System.currentTimeMillis(), eventMap);
                span.errorOccurred();
            }
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

    private String buildOperationName(String method, URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        return method + ":" + path;
    }

    private String getPeer(URI uri) {
        String host = uri.getHost();
        int port = uri.getPort();

        if (port == -1) {
            port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }

        return host + ":" + port;
    }
}
