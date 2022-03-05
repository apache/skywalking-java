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

package org.apache.skywalking.apm.plugin.vertx4;

import io.vertx.core.Context;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.impl.clustered.ClusteredMessage;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingPolicy;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SWVertxTracer implements VertxTracer<AbstractSpan, AbstractSpan> {

    @Override
    public <R> AbstractSpan receiveRequest(Context context, SpanKind kind, TracingPolicy policy, R request,
                                           String operation, Iterable<Map.Entry<String, String>> headers,
                                           TagExtractor<R> tagExtractor) {
        if (TracingPolicy.IGNORE.equals(policy)) {
            return null;
        }

        if (request instanceof HttpRequest) {
            HttpRequest serverRequest = (HttpRequest) request;

            ContextCarrier contextCarrier = getContextCarrier(headers);
            AbstractSpan span = toEntrySpan(
                    String.join(":", serverRequest.method().name(), serverRequest.uri()),
                    contextCarrier,
                    context
            );
            SpanLayer.asHttp(span);
            Tags.HTTP.METHOD.set(span, serverRequest.method().toString());
            Tags.URL.set(span, serverRequest.absoluteURI());

            return toAsyncSpan(context, span, false);
        } else if (request instanceof Message) {
            Message serverRequest = (Message) request;

            ContextCarrier contextCarrier = getContextCarrier(headers);
            AbstractSpan span = toEntrySpan(serverRequest.address(), contextCarrier, context);
            SpanLayer.asRPCFramework(span);

            return toAsyncSpan(context, span, false);
        }

        return null;
    }

    @Override
    public <R> void sendResponse(Context context, R response, AbstractSpan payload, Throwable failure,
                                 TagExtractor<R> tagExtractor) {
        if (payload != null) {
            if (failure != null) {
                payload.log(failure);
            }

            if (response instanceof HttpResponse) {
                Tags.HTTP_RESPONSE_STATUS_CODE.set(payload, ((HttpResponse) response).statusCode());
            }
            payload.asyncFinish();
        }
    }

    @Override
    public <R> AbstractSpan sendRequest(Context context, SpanKind kind, TracingPolicy policy, R request,
                                        String operation, BiConsumer<String, String> headers,
                                        TagExtractor<R> tagExtractor) {
        if (TracingPolicy.IGNORE.equals(policy) || request == null) {
            return null;
        }

        if (request instanceof HttpRequest) {
            HttpRequest clientRequest = (HttpRequest) request;

            ContextCarrier contextCarrier = new ContextCarrier();
            AbstractSpan span = toExitSpan(
                    clientRequest.uri(),
                    clientRequest.remoteAddress().host() + ":" + clientRequest.remoteAddress().port(),
                    contextCarrier,
                    context
            );
            SpanLayer.asHttp(span);
            Tags.HTTP.METHOD.set(span, clientRequest.method().name());
            Tags.URL.set(span, clientRequest.absoluteURI());

            return toExitAsyncSpan(context, headers, contextCarrier, span);
        } else if (request instanceof Message) {
            Message clientRequest = (Message) request;

            String remotePeer = "localhost";
            if (clientRequest instanceof ClusteredMessage) {
                EnhancedInstance enhancedInstance = (EnhancedInstance) clientRequest;
                if (enhancedInstance.getSkyWalkingDynamicField() != null) {
                    remotePeer = (String) enhancedInstance.getSkyWalkingDynamicField();
                }
            }

            ContextCarrier contextCarrier = new ContextCarrier();
            AbstractSpan span = toExitSpan(clientRequest.address(), remotePeer, contextCarrier, context);
            SpanLayer.asRPCFramework(span);

            return toExitAsyncSpan(context, headers, contextCarrier, span);
        }

        return null;
    }

    @Override
    public <R> void receiveResponse(Context context, R response, AbstractSpan payload, Throwable failure,
                                    TagExtractor<R> tagExtractor) {
        this.sendResponse(context, response, payload, failure, tagExtractor);
    }

    private void continueContextIfNecessary(Context context) {
        //Context.getLocal(String) changes to Context.getLocal(Object) from 4.0.x to 4.1.x, so direct access local map
        Map<Object, Object> contextMap = ((ContextInternal) context).localContextData();
        ContextSnapshot contextSnapshot = (ContextSnapshot) contextMap.get("sw.context-snapshot");

        if (contextSnapshot != null) {
            ContextManager.continued(contextSnapshot);
        }
    }

    private ContextCarrier getContextCarrier(Iterable<Map.Entry<String, String>> headers) {
        Map<String, String> headerMap = new HashMap<>();
        headers.forEach(it -> headerMap.put(it.getKey(), it.getValue()));
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(headerMap.get(next.getHeadKey()));
        }
        return contextCarrier;
    }

    private AbstractSpan toAsyncSpan(Context context, AbstractSpan span, boolean isExitSpan) {
        //Context.putLocal(String) changes to Context.putLocal(Object) from 4.0.x to 4.1.x, so direct access local map
        Map<Object, Object> contextMap = ((ContextInternal) context).localContextData();
        if (!isExitSpan) {
            contextMap.put("sw.context-snapshot", ContextManager.capture());
        }

        AbstractSpan asyncSpan = span.prepareForAsync();
        ContextManager.stopSpan();
        return asyncSpan;
    }

    private AbstractSpan toExitAsyncSpan(Context context, BiConsumer<String, String> headers,
                                         ContextCarrier contextCarrier, AbstractSpan span) {
        ContextManager.inject(contextCarrier);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            headers.accept(next.getHeadKey(), next.getHeadValue());
        }
        return toAsyncSpan(context, span, true);
    }

    private AbstractSpan toEntrySpan(String operationName, ContextCarrier contextCarrier, Context context) {
        AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
        continueContextIfNecessary(context);
        span.setComponent(ComponentsDefine.VERTX);
        return span;
    }

    private AbstractSpan toExitSpan(String operationName, String remotePeer, ContextCarrier contextCarrier,
                                    Context context) {
        AbstractSpan span = ContextManager.createExitSpan(operationName, contextCarrier, remotePeer);
        continueContextIfNecessary(context);
        span.setComponent(ComponentsDefine.VERTX);
        return span;
    }
}
