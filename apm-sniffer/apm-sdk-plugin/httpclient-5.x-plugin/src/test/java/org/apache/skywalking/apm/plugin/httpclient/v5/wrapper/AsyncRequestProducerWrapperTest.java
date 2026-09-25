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

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link AsyncRequestProducerWrapper} against the real {@link ContextManager}, via
 * {@link TracingSegmentRunner}. {@code startExitSpan()} calls {@code ContextManager.createExitSpan},
 * {@code AbstractSpan.prepareForAsync()} and {@code ContextManager.stopSpan()} directly — mocking those out
 * would only prove a mock was invoked, not that the caller's own active-span stack is left correctly balanced,
 * which is the entire point of this class (and of issue #14097).
 *
 * <p>{@code AsyncResponseConsumerWrapperTest} and {@code FutureCallbackWrapperTest} don't need this harness:
 * neither ever touches {@code ContextManager} — only the {@link AsyncRequestSpans} reference they're handed.
 *
 * <p><b>Known gap, deliberate:</b> there is no assertion here that the exit span's peer is built from the
 * explicit target host rather than the request URI's authority. That would require reading a completed span
 * back out of the archived {@code TraceSegment} (e.g. a peer accessor), and I don't have confirmed access to
 * that accessor in this codebase — guessing it once already produced a compile failure, so I'm not guessing
 * again. The target/URI precedence logic in {@code startExitSpan()} is a short, branch-free block that's easy
 * to verify by reading it directly; if you tell me the actual read-side accessor (on whatever class
 * {@code TraceSegment}/the span type actually exposes it), I'll add that assertion in a follow-up.
 */
@RunWith(TracingSegmentRunner.class)
public class AsyncRequestProducerWrapperTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    private static final HttpHost TARGET = new HttpHost("http", "example.org", 8080);

    /**
     * Stands in for every real {@code AsyncRequestProducer} (Internal/Minimal async clients, classic-facade
     * adapter): calls the {@link RequestChannel} it's handed synchronously, on the calling thread, with a
     * concrete request — exactly what {@link AsyncRequestProducerWrapper#sendRequest} depends on.
     */
        private AsyncRequestProducer syncDelegate(HttpRequest request) throws Exception {
        AsyncRequestProducer delegate = mock(AsyncRequestProducer.class);
        doAnswer(invocation -> {
            RequestChannel channel = invocation.getArgument(0);
            HttpContext context = invocation.getArgument(1);
            channel.sendRequest(request, null, context);
            return null;
        }).when(delegate).sendRequest(any(RequestChannel.class), any(HttpContext.class));
        return delegate;
    }

    private HttpRequest requestTo(String uri) throws Exception {
        return new BasicHttpRequest("GET", new URI(uri));
    }

    @Test
    public void callerSpanRemainsActiveImmediatelyAfterHandoff() throws Exception {
        AbstractSpan caller = ContextManager.createLocalSpan("caller");
        AsyncRequestSpans spans = new AsyncRequestSpans(TARGET);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(
            syncDelegate(requestTo("http://example.org/hello")), spans);

        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));

        assertSame(caller, ContextManager.activeSpan());

        ContextManager.stopSpan(caller);
        spans.finish();
    }

    @Test
    public void exitSpanIsDetachedAndNotArchivedUntilAsyncFinish() throws Exception {
        AbstractSpan outer = ContextManager.createLocalSpan("outer");
        AsyncRequestSpans spans = new AsyncRequestSpans(TARGET);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(
            syncDelegate(requestTo("http://example.org/hello")), spans);

        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));
        ContextManager.stopSpan(outer);

        assertEquals(0, segmentStorage.getTraceSegments().size());

        spans.finish();

        assertEquals(1, segmentStorage.getTraceSegments().size());
    }

    @Test
    public void headersAreInjectedIntoTheConcreteRequest() throws Exception {
        AbstractSpan outer = ContextManager.createLocalSpan("outer");
        HttpRequest request = requestTo("http://example.org/hello");
        AsyncRequestSpans spans = new AsyncRequestSpans(TARGET);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(syncDelegate(request), spans);

        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));

        assertTrue("sw8 propagation header must be injected", request.containsHeader("sw8"));

        ContextManager.stopSpan(outer);
        spans.finish();
    }

    @Test
    public void nestedInsideAnotherExitSpanDoesNotCreateASeparateAsyncSpan() throws Exception {
        AbstractSpan outerExit = ContextManager.createExitSpan("outer-exit", new ContextCarrier(), "outer-peer:1");

        AsyncRequestSpans spans = mock(AsyncRequestSpans.class);
        when(spans.getTarget()).thenReturn(TARGET);
        when(spans.claimCreation()).thenReturn(true);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(
            syncDelegate(requestTo("http://example.org/hello")), spans);

        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));

        verify(spans, never()).start(any(AbstractSpan.class));
        assertSame(outerExit, ContextManager.activeSpan());

        ContextManager.stopSpan(outerExit);
    }

    @Test
    public void sendRequestFromAnotherThreadNeverCreatesASpan() throws Exception {
        AbstractSpan outer = ContextManager.createLocalSpan("outer");
        AsyncRequestSpans spans = mock(AsyncRequestSpans.class);
        when(spans.getTarget()).thenReturn(TARGET);
        when(spans.claimCreation()).thenReturn(false);

        HttpRequest request = requestTo("http://example.org/hello");
        AsyncRequestProducer deferredDelegate = mock(AsyncRequestProducer.class);
        CountDownLatch done = new CountDownLatch(1);
        doAnswer(invocation -> {
            RequestChannel channel = invocation.getArgument(0);
            HttpContext context = invocation.getArgument(1);
            Thread t = new Thread(() -> {
                try {
                    channel.sendRequest(request, null, context);
                } catch (Exception ignored) {
                    // test-only best effort
                } finally {
                    done.countDown();
                }
            });
            t.start();
            return null;
        }).when(deferredDelegate).sendRequest(any(RequestChannel.class), any(HttpContext.class));

        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(deferredDelegate, spans);
        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));

        assertTrue(done.await(5, TimeUnit.SECONDS));
        verify(spans, never()).start(any(AbstractSpan.class));
        assertSame(outer, ContextManager.activeSpan());

        ContextManager.stopSpan(outer);
    }

    @Test
    public void tracingFailureInsideStartExitSpanNeverBreaksTheRealRequest() throws Exception {
        AbstractSpan outer = ContextManager.createLocalSpan("outer");
        HttpRequest badRequest = mock(HttpRequest.class);
        when(badRequest.getUri()).thenThrow(new java.net.URISyntaxException("x", "bad"));

        RequestChannel realChannel = mock(RequestChannel.class);
        AsyncRequestSpans spans = new AsyncRequestSpans(TARGET);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(syncDelegate(badRequest), spans);

        wrapper.sendRequest(realChannel, mock(HttpContext.class));
        verify(realChannel).sendRequest(eq(badRequest), any(), any(HttpContext.class));

        ContextManager.stopSpan(outer);
    }
}