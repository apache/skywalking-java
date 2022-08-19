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

package org.apache.skywalking.apm.plugin.micronaut.http.server;

import io.micronaut.http.HttpRequest;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

class MicronautCommons {
    static final String SPAN_KEY = "CORS_SPAN";
    static final String SKY_CONTEXT_SNAPSHOT_KEY = "CORS_SNAPSHOT";

    static void collectHttpParam(HttpRequest<?> httpRequest, AbstractSpan span) {
        String tag = httpRequest.getUri().getQuery();
        tag = MicronautHttpServerPluginConfig.Plugin.Http.HTTP_PARAMS_LENGTH_THRESHOLD > 0 ?
                StringUtil.cut(tag, MicronautHttpServerPluginConfig.Plugin.Http.HTTP_PARAMS_LENGTH_THRESHOLD) : tag;
        if (StringUtil.isNotEmpty(tag)) {
            Tags.HTTP.PARAMS.set(span, tag);
        }
    }

    static void cleanup(HttpRequest<?> request) {
        request.removeAttribute(MicronautCommons.SKY_CONTEXT_SNAPSHOT_KEY, ContextSnapshot.class);
        request.removeAttribute(MicronautCommons.SPAN_KEY, AbstractSpan.class);
    }

    static void beginTrace(HttpRequest<?> request) {
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(request.getHeaders().get(next.getHeadKey()));
        }
        String operationName = String.join(":", request.getMethod(), request.getPath());
        AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
        ContextSnapshot capture = ContextManager.capture();
        request.setAttribute(SPAN_KEY, span);
        request.setAttribute(SKY_CONTEXT_SNAPSHOT_KEY, capture);
        Tags.URL.set(span, String.format("%s://%s:%s%s", request.isSecure() ? "https" : "http", request.getServerName(), request.getServerAddress().getPort(), request.getUri().getPath()));
        Tags.HTTP.METHOD.set(span, request.getMethodName());
        span.setComponent(ComponentsDefine.MICRONAUT);
        SpanLayer.asHttp(span);
        span.prepareForAsync();
        ContextManager.stopSpan(span);
        if (MicronautHttpServerPluginConfig.Plugin.MicronautHttpServer.COLLECT_HTTP_PARAMS) {
            collectHttpParam(request, span);
        }
    }
}
