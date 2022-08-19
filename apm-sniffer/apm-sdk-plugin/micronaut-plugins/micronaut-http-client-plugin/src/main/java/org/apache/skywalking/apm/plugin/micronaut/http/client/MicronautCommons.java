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

package org.apache.skywalking.apm.plugin.micronaut.http.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.function.Consumer;

public class MicronautCommons {

    private static final String SPAN_KEY = "CORS_SPAN";
    private static final String SKY_CONTEXT_SNAPSHOT_KEY = "CORS_SNAPSHOT";

    static void beginTrace(MutableHttpRequest<?> request, URI requestURI) {
        String requestMethod = request.getMethod().name();
        AbstractSpan span = ContextManager.createExitSpan(requestMethod + ":" + request.getPath(), requestURI.getHost() + ":" + requestURI.getPort());
        //The key is set in micronaut-http-server-plugin . See org.apache.skywalking.apm.plugin.micronaut.http.server#beginTrace
        ServerRequestContext.currentRequest().flatMap(req -> req.getAttribute(SKY_CONTEXT_SNAPSHOT_KEY)).ifPresent(e -> ContextManager.continued((ContextSnapshot) e));
        final ContextCarrier contextCarrier = new ContextCarrier();
        ContextManager.inject(contextCarrier);
        span.setComponent(ComponentsDefine.MICRONAUT);
        Tags.HTTP.METHOD.set(span, requestMethod);
        Tags.URL.set(span, String.format("%s://%s:%s%s", requestURI.getScheme(), requestURI.getHost(), requestURI.getPort(), requestURI.getPath()));
        SpanLayer.asHttp(span);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            request.header(next.getHeadKey(), next.getHeadValue());
        }
        span.prepareForAsync();
        ContextManager.stopSpan(span);
        request.setAttribute(SPAN_KEY, span);
        if (MicronautHttpClientPluginConfig.Plugin.MicronautHttpClient.COLLECT_HTTP_PARAMS) {
            collectHttpParam(request, span);
        }
    }

    static Publisher<? extends HttpResponse<?>> buildTracePublisher(MutableHttpRequest<?> request, Publisher<? extends HttpResponse<?>> retPublisher) {
        return Flux.from(retPublisher).doOnError(ex -> finish(request, span -> span.log(ex).errorOccurred()))
                .doOnNext(resp ->
                        finish(request, span -> {
                            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, resp.code());
                            if (resp.code() >= 400) {
                                span.errorOccurred();
                            }
                            //Activate HTTP parameters collecting automatically in the profiling context.
                            if (!MicronautHttpClientPluginConfig.Plugin.MicronautHttpClient.COLLECT_HTTP_PARAMS && span.isProfiling()) {
                                collectHttpParam(request, span);
                            }
                        })
                );
    }

    static void collectHttpParam(MutableHttpRequest<?> httpRequest, AbstractSpan span) {
        String tag = httpRequest.getUri().getQuery();
        tag = MicronautHttpClientPluginConfig.Plugin.Http.HTTP_PARAMS_LENGTH_THRESHOLD > 0 ?
                StringUtil.cut(tag, MicronautHttpClientPluginConfig.Plugin.Http.HTTP_PARAMS_LENGTH_THRESHOLD) : tag;
        if (StringUtil.isNotEmpty(tag)) {
            Tags.HTTP.PARAMS.set(span, tag);
        }
    }

    static void finish(MutableHttpRequest<?> request, Consumer<AbstractSpan> action) {
        try {
            request.getAttribute(SPAN_KEY)
                    .map(span -> (AbstractSpan) span)
                    .ifPresent(span -> {
                        action.accept(span);
                        span.asyncFinish();
                    });
        } finally {
            request.removeAttribute(SPAN_KEY, AbstractSpan.class);
        }
    }

}
