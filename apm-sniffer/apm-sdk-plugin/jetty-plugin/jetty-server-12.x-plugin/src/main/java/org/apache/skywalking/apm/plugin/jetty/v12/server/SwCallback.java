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

package org.apache.skywalking.apm.plugin.jetty.v12.server;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.Invocable;

/**
 * Wraps the {@link Callback} passed to {@code Server#handle}. Jetty 12 request handling is async — the
 * response status is only known when the callback completes on the (possibly different) response thread.
 * On completion this reads the status, records it on the entry span and finishes the async span. It
 * delegates every method to the original callback so Jetty's threading model is preserved.
 */
public class SwCallback implements Callback {

    private final Callback delegate;
    private final Response response;
    private final AbstractSpan span;
    private final AtomicBoolean finished = new AtomicBoolean();

    public SwCallback(Callback delegate, Response response, AbstractSpan span) {
        this.delegate = delegate;
        this.response = response;
        this.span = span;
    }

    @Override
    public void succeeded() {
        finishOnce();
        delegate.succeeded();
    }

    @Override
    public void failed(Throwable x) {
        if (finished.compareAndSet(false, true)) {
            span.log(x);
            span.errorOccurred();
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, response.getStatus());
            span.asyncFinish();
        }
        delegate.failed(x);
    }

    @Override
    public Invocable.InvocationType getInvocationType() {
        return delegate.getInvocationType();
    }

    /**
     * Records the response status and finishes the async span exactly once. Safe to call from either the
     * callback completion or the interceptor's afterMethod fallback (not-handled / exception paths).
     */
    void finishOnce() {
        if (finished.compareAndSet(false, true)) {
            int statusCode = response.getStatus();
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
            if (statusCode >= 400) {
                span.errorOccurred();
            }
            span.asyncFinish();
        }
    }

    /**
     * Finishes the async span exactly once with an explicit status. Used for the not-handled / exception
     * fallback paths where Jetty writes the error status on the original callback after {@code handle()}
     * returns — so the wrapped callback never fires and {@link Response#getStatus()} is still 0 (uncommitted).
     */
    void finishWithStatus(int statusCode) {
        if (finished.compareAndSet(false, true)) {
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
            if (statusCode >= 400) {
                span.errorOccurred();
            }
            span.asyncFinish();
        }
    }
}
