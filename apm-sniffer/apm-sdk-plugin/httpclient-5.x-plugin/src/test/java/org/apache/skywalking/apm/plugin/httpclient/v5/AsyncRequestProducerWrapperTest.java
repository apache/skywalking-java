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
 */

package org.apache.skywalking.apm.plugin.httpclient.v5;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.AsyncRequestProducerWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(TracingSegmentRunner.class)
public class AsyncRequestProducerWrapperTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private AsyncRequestProducer producer;

    @Mock
    private RequestChannel requestChannel;

    @Before
    public void setUp() {
        ServiceManager.INSTANCE.boot();
    }

    @Test
    public void createsExitSpanAndInjectsPropagationHeader() throws Exception {
        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);

        HttpHost target = new HttpHost("http", "127.0.0.1", 8080);
        AsyncExitSpan exitSpan = new AsyncExitSpan(target);
        AsyncRequestProducerWrapper wrapper = new AsyncRequestProducerWrapper(producer, exitSpan);

        HttpRequest request = new BasicHttpRequest(
                "GET",
                "http://127.0.0.1:8080/hello"
        );
        HttpContext context = new BasicHttpContext();
        AtomicReference<HttpRequest> sentRequest = new AtomicReference<>();

        doAnswer(invocation -> {
            RequestChannel wrappedChannel = invocation.getArgument(0);

            wrappedChannel.sendRequest(request, null, context);
            return null;
        }).when(producer).sendRequest(any(RequestChannel.class), any(HttpContext.class));

        doAnswer(invocation -> {
            sentRequest.set(invocation.getArgument(0));
            return null;
        }).when(requestChannel).sendRequest(any(), any(), any());

        wrapper.sendRequest(requestChannel, context);

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));
        assertThat(sentRequest.get(), notNullValue());
        assertThat(sentRequest.get().getFirstHeader("sw8") != null, is(true));

        exitSpan.finish();

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));

        verify(producer).sendRequest(any(RequestChannel.class), any(HttpContext.class));
        verify(requestChannel).sendRequest(any(), any(), any());
    }

    @Test
    public void interleavedRequestsDoNotShareCallerSpanStack() throws Exception {
        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);

        AsyncExitSpan firstRequest = createAsyncExitSpan("/first");
        AsyncExitSpan secondRequest = createAsyncExitSpan("/second");

        Thread reactorThread = new Thread(() -> {
            // Both requests are already detached from the caller's span stack.
            // Finish them in reverse order to simulate response interleaving.
            secondRequest.onResponse(500);
            firstRequest.onResponse(200);

            secondRequest.finish();
            firstRequest.finish();

            // No request span should have been pushed onto this reactor thread.
            assertThat(ContextManager.isActive(), is(false));
        });

        reactorThread.start();
        reactorThread.join();

        // Finishing the async spans must not affect the caller's active span.
        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));

        List<AbstractTracingSpan> spans =
                SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));

        // One caller entry span + two independently finished request exit spans.
        assertThat(spans.size(), is(3));
    }

    private AsyncExitSpan createAsyncExitSpan(String operationName) {
        AsyncExitSpan exitSpan = new AsyncExitSpan(
                new HttpHost("http", "127.0.0.1", 8080)
        );

        AbstractSpan requestSpan = ContextManager.createExitSpan(
                operationName,
                new ContextCarrier(),
                "127.0.0.1:8080"
        );

        exitSpan.start(requestSpan);
        requestSpan.prepareForAsync();
        ContextManager.stopSpan(requestSpan);

        return exitSpan;
    }
}