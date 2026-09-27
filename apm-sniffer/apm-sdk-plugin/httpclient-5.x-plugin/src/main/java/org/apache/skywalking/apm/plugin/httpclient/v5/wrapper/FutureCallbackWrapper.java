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

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;

/**
 * This is the class the original bug (#14097) lived in: the old implementation called the parameterless
 * {@code ContextManager.stopSpan()} here, which pops whatever span is active on <em>whatever thread happens to
 * invoke this callback</em> — and with {@code HttpAsyncClients.classic(...)}, that can be the caller/business
 * thread, once it reads the response body to EOF. That thread's active span is the caller's own business span,
 * not this HTTP request's span.
 *
 * <p>This version never touches the active-span stack. It only finishes {@link #spans} by reference, which is
 * safe from any thread — the caller's own span is never at risk.
 *
 * <p>{@code completed}/{@code failed} are largely redundant with {@link AsyncResponseConsumerWrapper}'s own
 * finish paths ({@link AsyncRequestSpans#finish()}/{@link AsyncRequestSpans#fail(Throwable)} are idempotent), but
 * this callback still matters for {@link #cancelled()} — which the consumer never sees — and as a safety net for
 * any exchange that completes without ever driving the consumer's normal lifecycle.
 */
public class FutureCallbackWrapper<T> implements FutureCallback<T> {

    private final FutureCallback<T> callback;
    private final AsyncRequestSpans spans;

    public FutureCallbackWrapper(FutureCallback<T> callback, AsyncRequestSpans spans) {
        this.callback = callback;
        this.spans = spans;
    }

    @Override
    public void completed(T result) {
        spans.finish();
        if (callback != null) {
            callback.completed(result);
        }
    }

    @Override
    public void failed(Exception ex) {
        spans.fail(ex);
        if (callback != null) {
            callback.failed(ex);
        }
    }

    @Override
    public void cancelled() {
        spans.abort();
        if (callback != null) {
            callback.cancelled();
        }
    }
}
