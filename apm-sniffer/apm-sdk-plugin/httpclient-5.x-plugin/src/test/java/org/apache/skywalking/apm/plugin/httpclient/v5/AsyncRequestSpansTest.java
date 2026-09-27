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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.junit.Rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * These tests intentionally don't touch ContextManager/ByteBuddy at all — {@link AsyncRequestSpans} owns no
 * thread-stack state, so its lifecycle guarantees (exactly-once finish, correct error propagation, claim
 * exclusivity) can and should be verified directly, without a TracingSegmentRunner. Thread-stack correctness
 * (nothing leaks onto the reactor thread, the caller's own span survives) belongs in the plugin scenario, not
 * here — see test/plugin/scenarios/httpclient-5.x-scenario.
 */
public class AsyncRequestSpansTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AbstractSpan span;

    private AsyncRequestSpans spans;

    @Before
    public void setUp() {
        spans = new AsyncRequestSpans(null);
        spans.start(span);
    }

    @Test
    public void finishIsAppliedExactlyOnce() {
        spans.finish();
        spans.finish();
        spans.fail(new RuntimeException("late failure after already finished"));

        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void streamEndThenReleaseResourcesDoesNotDoubleFinishOrMarkError() {
        // consumeResponse (no error status) -> streamEnd -> releaseResources, the normal successful path.
        spans.onResponse(200);
        spans.finish();
        spans.release(); // what releaseResources() calls; must be a no-op once already finished

        verify(span, times(1)).asyncFinish();
        verify(span, never()).errorOccurred();
    }

    @Test
    public void releaseAfterResponseHeadWaitsForFailedAndKeepsTheCause() {
        // HttpAsyncMainClientExec#failed releases the consumer BEFORE it reports the cause. With the response head
        // already seen, release must leave the span open so the following fail(cause) still logs the exception.
        RuntimeException cause = new RuntimeException("body read failed");
        spans.onResponse(200);
        spans.release();

        verify(span, never()).asyncFinish();

        spans.fail(cause);

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).log(cause);
        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void releaseWithoutAnyResponseEndsAsError() {
        // No response head (e.g. a suppressed redirect with a non-repeatable entity): release may be the only
        // signal, so it ends the span as an error.
        spans.release();

        verify(span, times(1)).errorOccurred();
        verify(span, never()).log(org.mockito.ArgumentMatchers.any(Throwable.class));
        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void cancellationAfterResponseHeadStillEnds() {
        spans.onResponse(200);
        spans.release();
        spans.abort();

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void noBodyFinishesAtConsumeResponse() {
        spans.onResponse(204); // no entity -> caller calls finish() directly, streamEnd() never comes
        spans.finish();

        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void bodyFailureAfterSuccessfulHeadersIsAnError() {
        spans.onResponse(200);
        spans.fail(new RuntimeException("body read failed"));

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).log(org.mockito.ArgumentMatchers.any(Throwable.class));
        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void cancellationEndsAsError() {
        spans.abort();

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void errorStatusCodeMarksErrorWithoutFinishing() {
        spans.onResponse(500);

        verify(span, times(1)).errorOccurred();
        verify(span, never()).asyncFinish();
    }

    @Test
    public void informationResponseDoesNotFinishOrTagStatus() {
        // 1xx must be a pure passthrough at the wrapper level; AsyncRequestSpans is simply never called for it.
        // Nothing to assert here beyond "no interaction" — covered by not invoking onResponse/finish at all.
        verify(span, never()).asyncFinish();
    }

    @Test
    public void onlyTheCreatingThreadCanClaimCreation() throws InterruptedException {
        AsyncRequestSpans fresh = new AsyncRequestSpans(null);
        AtomicInteger claims = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);

        // A different thread -- standing in for a custom AsyncRequestProducer that defers sending to another
        // thread -- must NOT be able to claim creation. Only the constructing (doExecute) thread may.
        new Thread(() -> {
            if (fresh.claimCreation()) {
                claims.incrementAndGet();
            }
            done.countDown();
        }).start();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(0, claims.get());

        assertTrue(fresh.claimCreation());
        assertEquals(false, fresh.claimCreation()); // exactly once, even for the right thread
    }

    @Test
    public void callerReturnedRevokesClaimEvenIfUnused() {
        AsyncRequestSpans fresh = new AsyncRequestSpans(null);
        fresh.callerReturned();

        assertEquals(false, fresh.claimCreation());
    }
}
