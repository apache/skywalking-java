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
import org.apache.hc.core5.net.URIAuthority;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;

/**
 * Delegates every {@link AsyncRequestProducer} method unchanged, except {@link #sendRequest}, where it wraps the
 * {@link RequestChannel} the underlying producer is handed. All standard producers (Internal/Minimal async
 * clients, and the classic-facade adapter) call {@code channel.sendRequest(...)} synchronously, on the calling
 * thread, from inside {@code doExecute} — so this is where the concrete {@link HttpRequest} first becomes
 * available, while the caller's tracing context is still active.
 */
public class AsyncRequestProducerWrapper implements AsyncRequestProducer {

    private static final ILog LOGGER = LogManager.getLogger(AsyncRequestProducerWrapper.class);

    private final AsyncRequestProducer producer;
    private final AsyncRequestSpans spans;

    public AsyncRequestProducerWrapper(AsyncRequestProducer producer, AsyncRequestSpans spans) {
        this.producer = producer;
        this.spans = spans;
    }

    public AsyncRequestSpans getSpans() {
        return spans;
    }

    @Override
    public void sendRequest(RequestChannel channel, HttpContext context) throws HttpException, IOException {
        producer.sendRequest((request, entityDetails, ctx) -> {
            if (spans.claimCreation()) {
                try {
                    startExitSpan(request);
                } catch (Throwable t) {
                    // Tracing must never break the user's actual HTTP request.
                    LOGGER.error(t, "Failed to trace the async HttpClient request.");
                }
            }
            channel.sendRequest(request, entityDetails, ctx);
        }, context);
    }

    private void startExitSpan(HttpRequest request) throws URISyntaxException {
        // Resolve the destination the way the client does (InternalAbstractHttpAsyncClient with
        // RoutingSupport#determineHost): an explicit target wins, otherwise the request's scheme and authority.
        // The authority is used as is, not re-parsed through java.net.URI, whose getHost() is null for routable
        // names such as service_name.
        HttpHost target = spans.getTarget();
        String scheme;
        String host;
        int port;
        if (target != null) {
            scheme = target.getSchemeName();
            host = target.getHostName();
            port = target.getPort();
        } else {
            URIAuthority authority = request.getAuthority();
            if (authority == null) {
                return;
            }
            scheme = request.getScheme();
            host = authority.getHostName();
            port = authority.getPort();
        }
        if (host == null || host.isEmpty()) {
            return;
        }
        if (scheme == null) {
            scheme = "http";
        }
        if (port < 0) {
            port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }
        String peer = host + ":" + port;

        URI uri = request.getUri();
        String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
        // The URL tag keeps the path as it goes on the wire; the decoded path would turn %2F, %3F, %23 and %20
        // into /, ?, # and a space and change what the URL means.
        String rawPath = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        String url = scheme + "://" + peer + rawPath + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

        // Inside another plugin's exit span, createExitSpan reuses that span (depth + 1) instead of creating one.
        // That span belongs to the other plugin, so it must not be turned into an async span here: only propagate.
        boolean nested = ContextManager.activeSpan().isExit();
        // Create the span without a carrier and inject afterwards. createExitSpan(op, carrier, peer) injects before
        // returning, and injection throws for a reused outer exit span without a peer, which would leave the extra
        // depth on the caller's stack with nothing to stop it.
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
            // Detach before the request is forwarded: the client may report a failure on this thread before
            // doExecute returns. prepareForAsync() requires the span to still be the active one.
            if (!nested) {
                span.prepareForAsync();
            }
            ContextManager.stopSpan(span);
            if (!nested) {
                spans.start(span);
            }
        }
    }

    @Override
    public void failed(Exception cause) {
        producer.failed(cause);
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
    public void releaseResources() {
        producer.releaseResources();
    }
}
