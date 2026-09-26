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
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * This is the class the original bug (#14097) lived in. The old behavior called the parameterless
 * {@code ContextManager.stopSpan()} here, which is exactly what these tests exist to guard against ever
 * regressing to: none of them touch {@code ContextManager} at all, they only assert the {@link AsyncRequestSpans}
 * reference is finished by reference — which is what actually makes it safe to run on the caller/business
 * thread, as {@code HttpAsyncClients.classic(...)} does.
 */
@RunWith(MockitoJUnitRunner.class)
public class FutureCallbackWrapperTest {

    @Mock
    private AbstractSpan span;

    @Mock
    private FutureCallback<String> delegate;

    private AsyncRequestSpans spans;

    @Before
    public void setUp() {
        spans = new AsyncRequestSpans(null);
        spans.start(span);
    }

    @Test
    public void completedFinishesSpanExactlyOnceAndDelegates() {
        FutureCallbackWrapper<String> wrapper = new FutureCallbackWrapper<>(delegate, spans);

        wrapper.completed("result");

        verify(span, times(1)).asyncFinish();
        verify(span, never()).errorOccurred();
        verify(delegate).completed("result");
    }

    @Test
    public void failedMarksErrorAndFinishesExactlyOnceAndDelegates() {
        FutureCallbackWrapper<String> wrapper = new FutureCallbackWrapper<>(delegate, spans);
        RuntimeException cause = new RuntimeException("boom");

        wrapper.failed(cause);

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).log(cause);
        verify(span, times(1)).asyncFinish();
        verify(delegate).failed(cause);
    }

    @Test
    public void cancelledMarksErrorAndFinishesExactlyOnceAndDelegates() {
        FutureCallbackWrapper<String> wrapper = new FutureCallbackWrapper<>(delegate, spans);

        wrapper.cancelled();

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).asyncFinish();
        verify(delegate).cancelled();
    }

    @Test
    public void toleratesANullDelegateCallback() {
        // doExecute is always wrapped even when the caller passed no callback of their own — this is the only
        // lifecycle hook that observes cancellation, so it must not NPE on a null delegate.
        FutureCallbackWrapper<String> wrapper = new FutureCallbackWrapper<>(null, spans);

        wrapper.completed("result"); // must not throw

        verify(span, times(1)).asyncFinish();
    }

    @Test
    public void completedAfterConsumerAlreadyFinishedDoesNotDoubleFinish() {
        // Simulates AsyncResponseConsumerWrapper already having finished the span (streamEnd/consumeResponse)
        // before the future callback also fires — FutureCallbackWrapper's own paths are largely redundant
        // safety nets, and AsyncRequestSpans' idempotency is what makes that redundancy safe.
        spans.finish(); // as if AsyncResponseConsumerWrapper already ran
        FutureCallbackWrapper<String> wrapper = new FutureCallbackWrapper<>(delegate, spans);

        wrapper.completed("result");

        verify(span, times(1)).asyncFinish();
        verify(delegate).completed("result");
    }

    @Test
    public void cancelledAfterAlreadyFailedDoesNotOverwriteOrDoubleFinish() {
        FutureCallbackWrapper<String> wrapper = new FutureCallbackWrapper<>(delegate, spans);
        wrapper.failed(new RuntimeException("first"));

        wrapper.cancelled();

        verify(span, times(1)).errorOccurred();
        verify(span, times(1)).asyncFinish();
        verify(delegate).cancelled();
    }
}
