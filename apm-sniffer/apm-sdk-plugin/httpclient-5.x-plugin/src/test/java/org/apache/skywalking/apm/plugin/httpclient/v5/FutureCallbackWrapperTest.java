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

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.AsyncResponseConsumerWrapper;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.FutureCallbackWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class FutureCallbackWrapperTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FutureCallback<String> delegate;

    @Mock
    private AsyncResponseConsumer<String> consumer;
    @Mock
    private HttpResponse response;

    private HttpContext httpContext;

    @Before
    public void setUp() {
        ServiceManager.INSTANCE.boot();
        httpContext = new BasicHttpContext();
    }

    @Test
    public void completedOnIoThreadStopsOwnedSpan() {
        AbstractSpan owned = ContextManager.createLocalSpan("httpasyncclient/local");
        httpContext.setAttribute(Constants.SKYWALKING_LOCAL_SPAN, owned);

        new FutureCallbackWrapper<>(delegate, httpContext).completed("ok");

        assertThat(ContextManager.isActive(), is(false));
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        verify(delegate).completed("ok");
    }

    @Test
    public void failedOnIoThreadMarksOwnedSpanAsError() {
        AbstractSpan owned = ContextManager.createLocalSpan("httpasyncclient/local");
        httpContext.setAttribute(Constants.SKYWALKING_LOCAL_SPAN, owned);
        Exception cause = new RuntimeException("boom");

        new FutureCallbackWrapper<>(delegate, httpContext).failed(cause);

        assertThat(ContextManager.isActive(), is(false));
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));
        assertThat(SpanHelper.getErrorOccurred(spans.get(0)), is(true));
        verify(delegate).failed(cause);
    }

    @Test
    public void completedOnCallerThreadKeepsCallerSpanActive() throws Exception {
        // The request span is created on an "I/O thread", as IOSessionImplPollInterceptor does.
        Thread ioThread = new Thread(() -> {
            AbstractSpan owned = ContextManager.createLocalSpan("httpasyncclient/local");
            httpContext.setAttribute(Constants.SKYWALKING_LOCAL_SPAN, owned);
        });
        ioThread.start();
        ioThread.join();

        // The callback then runs on the caller/business thread, which has its own active Entry span.
        AbstractSpan entry = ContextManager.createEntrySpan("/business", null);

        new FutureCallbackWrapper<>(delegate, httpContext).completed("ok");

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == entry, is(true));
        verify(delegate).completed("ok");

        ContextManager.stopSpan(entry);
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        assertThat(SegmentHelper.getSpans(segment).size(), is(1));
    }

    /**
     * Full lifecycle of the reported scenario: spans are created and finished on the I/O thread, the FutureCallback
     * then runs on the business thread which owns an Entry span that must survive.
     */
    @Test
    public void ioThreadSegmentIsFinishedAndCallerEntrySpanSurvives() throws Exception {
        when(response.getCode()).thenReturn(200);
        Throwable[] ioError = new Throwable[1];
        Thread ioThread = new Thread(() -> {
            try {
                // what IOSessionImplPollInterceptor does
                AbstractSpan local = ContextManager.createLocalSpan("httpasyncclient/local");
                httpContext.setAttribute(Constants.SKYWALKING_LOCAL_SPAN, local);
                ContextManager.createExitSpan("/hello", new ContextCarrier(), "127.0.0.1:8080");
                // response headers arrive on the I/O thread
                new AsyncResponseConsumerWrapper<>(consumer).consumeResponse(response, null, httpContext, null);
            } catch (Throwable t) {
                ioError[0] = t;
            }
        });
        ioThread.start();
        ioThread.join();
        assertThat(ioError[0] == null, is(true));

        // local + exit span are both finished, so the I/O thread's segment is complete
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        assertThat(SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0)).size(), is(2));

        // entity read to EOF on the business thread triggers the callback there
        AbstractSpan entry = ContextManager.createEntrySpan("/business", null);
        new FutureCallbackWrapper<>(delegate, httpContext).completed("body");

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == entry, is(true));
        ContextManager.stopSpan(entry);

        assertThat(segmentStorage.getTraceSegments().size(), is(2));
        assertThat(SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(1)).size(), is(1));
        verify(delegate).completed("body");
    }

    @Test
    public void nullContextNeverStopsForeignSpan() {
        AbstractSpan entry = ContextManager.createEntrySpan("/business", null);

        new FutureCallbackWrapper<>(delegate, null).completed("ok");

        assertThat(ContextManager.isActive(), is(true));
        ContextManager.stopSpan(entry);
        verify(delegate).completed("ok");
    }
}
