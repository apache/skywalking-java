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
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;

/**
 * Runs entirely on the I/O thread (with the sole exception that {@code releaseResources()} can also be invoked
 * from elsewhere during cleanup). Never touches {@code ContextManager}'s active-span stack — only ever tags or
 * finishes {@link #spans} by reference, which is safe to do from any thread.
 */
public class AsyncResponseConsumerWrapper<T> implements AsyncResponseConsumer<T> {

    private final AsyncResponseConsumer<T> consumer;
    private final AsyncRequestSpans spans;

    public AsyncResponseConsumerWrapper(AsyncResponseConsumer<T> consumer, AsyncRequestSpans spans) {
        this.consumer = consumer;
        this.spans = spans;
    }

    @Override
    public void consumeResponse(HttpResponse response, EntityDetails entityDetails, HttpContext context,
        org.apache.hc.core5.concurrent.FutureCallback<T> resultCallback) throws HttpException, IOException {
        spans.onResponse(response.getCode());
        if (entityDetails == null) {
            // No body means streamEnd() will never be called for this exchange.
            spans.finish();
        }
        consumer.consumeResponse(response, entityDetails, context, resultCallback);
    }

    @Override
    public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
        // 1xx is not the final response; the exit span's status must come from the final consumeResponse() call.
        consumer.informationResponse(response, context);
    }

    @Override
    public void streamEnd(java.util.List<? extends org.apache.hc.core5.http.Header> trailers)
        throws HttpException, IOException {
        spans.finish();
        consumer.streamEnd(trailers);
    }

    @Override
    public void failed(Exception cause) {
        spans.fail(cause);
        consumer.failed(cause);
    }

    @Override
    public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
        consumer.updateCapacity(capacityChannel);
    }

    @Override
    public void consume(java.nio.ByteBuffer src) throws IOException {
        consumer.consume(src);
    }

    @Override
    public void releaseResources() {
        // Fallback finisher, not a success signal: HttpAsyncMainClientExec#failed calls releaseResources()
        // *before* reporting the failure, and a suppressed-redirect-with-non-repeatable-entity exchange only
        // ever calls completed() without a real response. abort() only takes effect if the span is still open —
        // every normal-completion path above has already finished it by the time release runs, so this is then
        // a no-op. If the span IS still open here, the exchange ended without a complete response, so it's
        // correctly marked as an error rather than silently dropped.
        spans.abort();
        consumer.releaseResources();
    }

    // NOTE FOR AYUSH: same caveat as AsyncRequestProducerWrapper — let the IDE fill in any interface method not
    // listed above (e.g. some httpcore5 versions' AsyncResponseConsumer exposes it slightly differently); every
    // one you add should be a plain delegate to `consumer` with zero span logic. The five methods above
    // (consumeResponse, informationResponse, streamEnd, failed, releaseResources) are the only ones that matter
    // for span lifecycle.
}
