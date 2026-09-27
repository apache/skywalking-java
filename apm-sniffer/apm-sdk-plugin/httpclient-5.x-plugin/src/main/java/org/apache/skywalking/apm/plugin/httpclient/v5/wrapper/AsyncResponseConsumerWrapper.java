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
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;

/**
 * Finishes the request's exit span through {@link AsyncRequestSpans}, never through the span stack of the current
 * thread: the response is consumed on the I/O reactor thread, which serves many requests, and for
 * {@code HttpAsyncClients.classic(...)} the body may be read to the end on the caller's thread.
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
        FutureCallback<T> resultCallback) throws HttpException, IOException {
        spans.onResponse(response.getCode());
        consumer.consumeResponse(response, entityDetails, context,
            resultCallback == null ? null : new ResultCallback<>(resultCallback, spans));
        if (entityDetails == null) {
            // No body means streamEnd() will never be called for this exchange. Finish only once the consumer has
            // processed the response: if it throws, the client reports the cause through failed(cause).
            spans.finish();
        }
    }

    @Override
    public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
        // 1xx is not the final response; the exit span's status must come from the final consumeResponse() call.
        consumer.informationResponse(response, context);
    }

    @Override
    public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
        // The consumer decodes the body and builds the result at EOF, and that can fail. Finish as a success only
        // after it returned normally: a failure it throws reaches failed(cause), and a failure it reports through
        // the result callback reaches ResultCallback#failed, both of which end the span with the cause.
        consumer.streamEnd(trailers);
        spans.finish();
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
    public void consume(ByteBuffer src) throws IOException {
        consumer.consume(src);
    }

    @Override
    public void releaseResources() {
        // Not a success signal. Normally the span was already finished at streamEnd/consumeResponse and this does
        // nothing. HttpAsyncMainClientExec#failed and H2AsyncMainClientExec#failed release the consumer before
        // they report the cause, so once the response head was seen the span stays open here and failed(cause)
        // right after ends it with the exception logged. Only when no response head was seen does release end the
        // span as an error, because it can be the only signal (a suppressed redirect with a non-repeatable entity
        // in 5.5.x never calls failed or completed).
        spans.release();
        consumer.releaseResources();
    }

    /**
     * The callback the consumer reports its result through. A failure there (e.g. an {@code EOFException} while
     * building the response at {@code streamEnd}) ends the span with the cause. Success is left to
     * {@code streamEnd}/{@code consumeResponse}, so the moment the span finishes does not change.
     */
    private static final class ResultCallback<T> implements FutureCallback<T> {

        private final FutureCallback<T> callback;
        private final AsyncRequestSpans spans;

        ResultCallback(FutureCallback<T> callback, AsyncRequestSpans spans) {
            this.callback = callback;
            this.spans = spans;
        }

        @Override
        public void completed(T result) {
            callback.completed(result);
        }

        @Override
        public void failed(Exception ex) {
            spans.fail(ex);
            callback.failed(ex);
        }

        @Override
        public void cancelled() {
            spans.abort();
            callback.cancelled();
        }
    }
}
