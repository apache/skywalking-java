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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncRequestSpans;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * Runs {@link AsyncRequestProducerWrapper} against the real {@link ContextManager}: the exit span must be created in
 * the caller's segment, detached from the caller's stack before the request is forwarded, and finished later by
 * reference only.
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

    @Test
    public void exitSpanLandsInCallerSegmentWithExplicitTargetAsPeer() throws Exception {
        AbstractSpan caller = ContextManager.createLocalSpan("caller");
        // The request's own authority differs from the explicit target, which must win, as in the client itself.
        AsyncRequestSpans spans = new AsyncRequestSpans(TARGET);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(
            syncDelegate(requestTo("http://other.invalid:9999/hello?a=b")), spans);

        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));
        spans.onResponse(200);
        spans.finish();
        ContextManager.stopSpan(caller);

        assertEquals(1, segmentStorage.getTraceSegments().size());
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spanList = SegmentHelper.getSpans(segment);
        assertEquals(2, spanList.size());
        AbstractTracingSpan exit = spanList.get(0);
        assertTrue(exit.isExit());
        assertEquals("/hello", exit.getOperationName());
        assertEquals("example.org:8080", SpanHelper.getPeer(exit));
        assertEquals(caller.getSpanId(), SpanHelper.getParentSpanId(exit));
        assertFalse(SpanHelper.getErrorOccurred(exit));
    }

    /**
     * Without an explicit target the client routes by the request's scheme and authority. {@code service_name} is
     * routable (e.g. through a custom DNS resolver) but java.net.URI does not parse it as a host, so the destination
     * must come from the authority, not from {@code URI.getHost()}.
     */
    @Test
    public void routableAuthorityThatUriCannotParseIsStillTraced() throws Exception {
        AbstractSpan caller = ContextManager.createLocalSpan("caller");
        HttpRequest request = new BasicHttpRequest("GET", "/hello?a=b");
        request.setScheme("http");
        request.setAuthority(new URIAuthority("service_name", 8080));
        AsyncRequestSpans spans = new AsyncRequestSpans(null);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(syncDelegate(request), spans);

        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));

        assertTrue("sw8 propagation header must be injected", request.containsHeader("sw8"));
        spans.onResponse(200);
        spans.finish();
        ContextManager.stopSpan(caller);

        AbstractTracingSpan exit = SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0)).get(0);
        assertTrue(exit.isExit());
        assertEquals("/hello", exit.getOperationName());
        assertEquals("service_name:8080", SpanHelper.getPeer(exit));
        assertEquals("http://service_name:8080/hello?a=b", tag(exit, "url"));
    }

    @Test
    public void noTargetAndNoAuthorityCreatesNoSpan() throws Exception {
        AbstractSpan caller = ContextManager.createLocalSpan("caller");
        HttpRequest request = new BasicHttpRequest("GET", "/hello");
        RequestChannel realChannel = mock(RequestChannel.class);
        AsyncRequestSpans spans = new AsyncRequestSpans(null);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(syncDelegate(request), spans);

        wrapper.sendRequest(realChannel, mock(HttpContext.class));

        verify(realChannel).sendRequest(eq(request), any(), any(HttpContext.class));
        assertFalse(request.containsHeader("sw8"));
        assertSame(caller, ContextManager.activeSpan());
        ContextManager.stopSpan(caller);
    }

    /**
     * The URL tag must keep the path encoded as it is sent; decoding it would turn %2F, %3F, %23 and %20 into
     * characters that change what the URL means. The operation name keeps using the decoded path.
     */
    @Test
    public void urlTagKeepsTheEncodedPath() throws Exception {
        AbstractSpan caller = ContextManager.createLocalSpan("caller");
        AsyncRequestSpans spans = new AsyncRequestSpans(TARGET);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(
            syncDelegate(requestTo("http://example.org:8080/a%2Fb%3Fc%23d%20e?q=value")), spans);

        wrapper.sendRequest(mock(RequestChannel.class), mock(HttpContext.class));
        spans.finish();
        ContextManager.stopSpan(caller);

        AbstractTracingSpan exit = SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0)).get(0);
        assertEquals("http://example.org:8080/a%2Fb%3Fc%23d%20e?q=value", tag(exit, "url"));
        assertEquals("/a/b?c#d e", exit.getOperationName());
    }

    private static String tag(AbstractTracingSpan span, String key) {
        for (TagValuePair pair : SpanHelper.getTags(span)) {
            if (key.equals(pair.getKey().key())) {
                return pair.getValue();
            }
        }
        return null;
    }

    /**
     * An outer exit span without a peer: injection fails, but the reused span's extra depth must still be released,
     * otherwise the outer span could never be stopped and the caller's segment would never be reported.
     */
    @Test
    public void nestedInsideExitSpanWithoutPeerLeavesTheStackBalanced() throws Exception {
        AbstractSpan outerExit = ContextManager.createExitSpan("outer-exit", "");
        HttpRequest request = requestTo("http://example.org/hello");
        RequestChannel realChannel = mock(RequestChannel.class);
        AsyncRequestSpans spans = new AsyncRequestSpans(TARGET);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(syncDelegate(request), spans);

        wrapper.sendRequest(realChannel, mock(HttpContext.class));

        verify(realChannel).sendRequest(eq(request), any(), any(HttpContext.class));
        assertSame(outerExit, ContextManager.activeSpan());
        ContextManager.stopSpan(outerExit);
        assertEquals(1, segmentStorage.getTraceSegments().size());
    }
}
