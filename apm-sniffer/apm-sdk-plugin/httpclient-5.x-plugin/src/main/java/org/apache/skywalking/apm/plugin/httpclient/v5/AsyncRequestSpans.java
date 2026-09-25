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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.core5.http.HttpHost;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

/**
 * Per-request async exit span, owned by the request itself rather than by whatever thread happens to be running
 * when a callback fires.
 *
 * <p>The span is created once, on the caller thread inside {@code doExecute}, while the caller's tracing context is
 * still active. It is then immediately detached via {@link AbstractSpan#prepareForAsync()} +
 * {@code ContextManager.stopSpan(span)} so it never sits on any thread's active-span stack while the request is in
 * flight. From that point on it is finished exactly once, by reference, from whichever lifecycle callback gets
 * there first (I/O thread response consumer, or the future callback on the caller/business thread) — never by a
 * parameterless {@code ContextManager.stopSpan()} that would blindly pop whatever span is currently active on that
 * thread.
 *
 * <p>All mutating operations are synchronized: {@link #onResponse(int)} (tagging, typically the I/O thread) can
 * otherwise race with {@link #finish()} / {@link #fail(Throwable)} (typically the response-consumer or callback
 * thread) finishing and clearing the span in the same window. An {@link AtomicReference} alone would prevent a
 * double-finish but not a tag-write racing a finish.
 */
public class AsyncRequestSpans {

    private final HttpHost target;

    /**
     * Only true, and only once, on the thread that is still inside {@code doExecute} when the request producer
     * hands the concrete request to the channel. Any other thread (a custom {@code AsyncRequestProducer} that
     * defers sending) has no relationship to the caller's context, so it must not create a span.
     */
    private final AtomicReference<Thread> creator = new AtomicReference<>(Thread.currentThread());

    private AbstractSpan span;
    private boolean finished;

    public AsyncRequestSpans(HttpHost target) {
        this.target = target;
    }

    public HttpHost getTarget() {
        return target;
    }

    /**
     * Claims the right to create the span. Returns {@code true} at most once, and only for the thread that
     * constructed this holder (the {@code doExecute} caller thread).
     */
    public boolean claimCreation() {
        return creator.compareAndSet(Thread.currentThread(), null);
    }

    /**
     * Called at the end of {@code doExecute} (success or failure) so a late/duplicate send from the same thread
     * cannot still claim creation after the caller has moved on.
     */
    public void callerReturned() {
        creator.set(null);
    }

    /**
     * Stores the span. Must be called only after the span has already been detached with
     * {@code prepareForAsync()} + {@code ContextManager.stopSpan(span)} — this class never touches the active-span
     * stack itself.
     */
    public synchronized void start(AbstractSpan span) {
        this.span = span;
    }

    public synchronized void onResponse(int statusCode) {
        if (span == null || finished) {
            return;
        }

        Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);

        if (statusCode >= 400) {
            span.errorOccurred();
        }
    }

    /** The whole response completed successfully. */
    public synchronized void finish() {
        end(false, null);
    }

    /** The exchange failed with an exception. */
    public synchronized void fail(Throwable cause) {
        end(true, cause);
    }

    /**
     * Cancelled, or resources released before the response ever completed (e.g. a redirect exec that declines to
     * resend a non-repeatable entity and never invokes {@code completed()}). Only takes effect if the span is
     * still open — the normal-completion paths already finished it earlier, so this is then a no-op.
     */
    public synchronized void abort() {
        end(true, null);
    }

    private void end(boolean error, Throwable cause) {
        if (span == null || finished) {
            return;
        }

        finished = true;

        if (error) {
            span.errorOccurred();
        }

        if (cause != null) {
            span.log(cause);
        }

        span.asyncFinish();
        span = null;
    }
}