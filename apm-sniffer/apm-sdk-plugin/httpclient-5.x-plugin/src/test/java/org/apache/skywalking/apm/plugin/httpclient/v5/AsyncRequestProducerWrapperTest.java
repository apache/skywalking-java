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

import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
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
}