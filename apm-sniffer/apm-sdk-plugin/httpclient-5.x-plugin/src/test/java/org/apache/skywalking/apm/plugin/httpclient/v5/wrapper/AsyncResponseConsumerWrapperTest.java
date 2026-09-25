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

import java.util.Collections;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AsyncResponseConsumerWrapper} never touches {@code ContextManager} — it only tags/finishes the
 * {@link AsyncRequestSpans} reference it's given, so these tests wire a REAL {@code AsyncRequestSpans} to a
 * MOCKED {@link AbstractSpan}, the same technique {@code AsyncRequestSpansTest} uses. That gives two things at
 * once per test: proof the wrapped consumer's original behavior is still invoked unchanged (delegation), and
 * proof of the actual span-lifecycle side effect through the real holder (not just "a mock was called").
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncResponseConsumerWrapperTest {

    @Mock
    private AbstractSpan span;

    @Mock
    private AsyncResponseConsumer<Object> delegate;

    @Mock
    private FutureCallback<Object> resultCallback;

    private AsyncRequestSpans spans;
    private AsyncResponseConsumerWrapper<Object> wrapper;
    private HttpContext context;

    @Before
    public void setUp() {
        spans = new AsyncRequestSpans(null);
        spans.start(span);
        wrapper = new AsyncResponseConsumerWrapper<>(delegate, spans);
        context = new HttpCoreContext();
    }

    private HttpResponse response(int status) {
        HttpResponse r = mock(HttpResponse.class);
        when(r.getCode()).thenReturn(status);
        return r;
    }

    @Test
    public void consumeResponseWithEntityTagsStatusButDoesNotFinishYet() throws Exception {
        EntityDetails entity = mock(EntityDetails.class);
        HttpResponse response = response(200);

        wrapper.consumeResponse(response, entity, context, resultCallback);

        verify(span, never()).asyncFinish();
        verify(delegate).consumeResponse(response, entity, context, resultCallback);
    }

    @Test
    public void consumeResponseWithoutEntityFinishesImmediately() throws Exception {
        // e.g. a 204 with no body: streamEnd() will never be called for this exchange, so consumeResponse()
        // itself must finish the span.
        HttpResponse response = response(204);

        wrapper.consumeResponse(response, null, context, resultCallback);

        verify(span, times(1)).asyncFinish();
        verify(delegate).consumeResponse(response, null, context, resultCallback);
    }

    @Test
    public void errorStatusMarksErrorWithoutFinishing() throws Exception {
        EntityDetails entity = mock(EntityDetails.class);
        HttpResponse response = response(500);

        wrapper.consumeResponse(response, entity, context, resultCallback);

        verify(span, times(1)).errorOccurred();
        verify(span, never()).asyncFinish();
    }

    @Test
    public void informationResponseNeverTouchesTheSpan() throws Exception {
        HttpResponse response = response(100);

        wrapper.informationResponse(response, context);

        verify(span, never()).asyncFinish();
        verify(span, never()).errorOccurred();
        verify(delegate).informationResponse(response, context);
    }

    @Test
    public void streamEndFinishesTheSpanExactlyOnce() throws Exception {
        wrapper.streamEnd(Collections.emptyList());

        verify(span, times(1)).asyncFinish();
        verify(delegate).streamEnd(Collections.emptyList());
    }

    @Test
    public void consumeResponseThenStreamEndFinishesExactlyOnce() throws Exception {
        EntityDetails entity = mock(EntityDetails.class);
        wrapper.consumeResponse(response(200), entity, context, resultCallback);
        wrapper.streamEnd(Collections.emptyList());

        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void failedMarksErrorAndFinishesExactlyOnce() {
        RuntimeException cause = new RuntimeException("boom");

        wrapper.failed(cause);

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).log(cause);
        verify(span, times(1)).asyncFinish();
        verify(delegate).failed(cause);
    }

    @Test
    public void releaseResourcesAfterNormalCompletionIsANoOp() throws Exception {
        wrapper.streamEnd(Collections.emptyList());
        wrapper.releaseResources();

        // finish() already ran at streamEnd(); releaseResources()'s abort() must not run it a second time nor
        // retroactively mark a successful exchange as an error.
        verify(span, times(1)).asyncFinish();
        verify(span, never()).errorOccurred();
        verify(delegate).releaseResources();
    }

    @Test
    public void releaseResourcesBeforeFailedStillEndsAsErrorExactlyOnce() {
        // HttpAsyncMainClientExec#failed calls releaseResources() BEFORE reporting the real failure.
        wrapper.releaseResources();
        wrapper.failed(new RuntimeException("real cause, arrives after release"));

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void releaseResourcesWithoutAnyResponseEndsAsError() {
        // A suppressed redirect with a non-repeatable entity: only releaseResources() ever runs, failed()/
        // completed() never do. The span must still end, and must end as an error (the exchange never actually
        // completed), not silently disappear.
        wrapper.releaseResources();

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void updateCapacityAndConsumeAreTransparentPassthroughs() throws Exception {
        CapacityChannel capacityChannel = mock(CapacityChannel.class);
        wrapper.updateCapacity(capacityChannel);
        verify(delegate).updateCapacity(capacityChannel);
        verify(span, never()).asyncFinish();

        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(0);
        wrapper.consume(buf);
        verify(delegate).consume(buf);
        verify(span, never()).asyncFinish();
    }
}
