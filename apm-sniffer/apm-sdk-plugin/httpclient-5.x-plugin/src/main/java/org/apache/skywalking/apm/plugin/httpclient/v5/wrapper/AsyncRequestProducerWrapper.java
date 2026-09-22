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
 */

package org.apache.skywalking.apm.plugin.httpclient.v5.wrapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncExitSpan;

public class AsyncRequestProducerWrapper implements AsyncRequestProducer {

    private static final ILog LOGGER = LogManager.getLogger(AsyncRequestProducerWrapper.class);

    private final AsyncRequestProducer producer;
    private final AsyncExitSpan exitSpan;

    public AsyncRequestProducerWrapper(AsyncRequestProducer producer, AsyncExitSpan exitSpan) {
        this.producer = producer;
        this.exitSpan = exitSpan;
    }

    public AsyncExitSpan getExitSpan() {
        return exitSpan;
    }

    @Override
    public void sendRequest(RequestChannel channel, HttpContext context) throws IOException, HttpException {
        producer.sendRequest((request, entityDetails, requestContext) -> {
            if (exitSpan.claimCreation()) {
                try {
                    startExitSpan(request);
                } catch (Throwable t) {
                    LOGGER.error("Failed to trace the async HTTP request.", t);
                }
            }

            channel.sendRequest(request, entityDetails, requestContext);
        }, context);
    }

    private void startExitSpan(HttpRequest request) throws URISyntaxException {
        URI uri = request.getUri();
        HttpHost target = exitSpan.getTarget();

        String scheme = target != null ? target.getSchemeName() : uri.getScheme();
        String host = target != null ? target.getHostName() : uri.getHost();
        int port = target != null ? target.getPort() : uri.getPort();

        if (host == null) {
            return;
        }

        if (scheme == null) {
            scheme = "http";
        }

        if (port < 0) {
            port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }

        String peer = host + ":" + port;

        String path = uri.getPath() == null || uri.getPath().isEmpty()
                ? "/"
                : uri.getPath();

        String url = scheme + "://" + peer + path
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

        /*
         * Check whether an exit span was already active BEFORE creating
         * this request's span.
         */
        boolean nested = ContextManager.isActive() && ContextManager.activeSpan().isExit();

        AbstractSpan span = ContextManager.createExitSpan(path, peer);

        try {
            if (!nested) {
                span.setComponent(ComponentsDefine.HTTP_ASYNC_CLIENT);
                Tags.URL.set(span, url);
                Tags.HTTP.METHOD.set(span, request.getMethod());
                SpanLayer.asHttp(span);
            }

            ContextCarrier carrier = new ContextCarrier();
            ContextManager.inject(carrier);

            CarrierItem next = carrier.items();
            while (next.hasNext()) {
                next = next.next();
                request.setHeader(next.getHeadKey(), next.getHeadValue());
            }
        } finally {
            if (!nested) {
                span.prepareForAsync();
            }

            ContextManager.stopSpan(span);

            if (!nested) {
                exitSpan.start(span);
            }
        }
    }

    @Override
    public boolean isRepeatable() {
        return producer.isRepeatable();
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        producer.produce(channel);
    }

    @Override
    public int available() {
        return producer.available();
    }

    @Override
    public void failed(Exception cause) {
        producer.failed(cause);
    }

    @Override
    public void releaseResources() {
        producer.releaseResources();
    }
}