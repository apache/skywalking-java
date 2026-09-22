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
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.AsyncResponseConsumerWrapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class AsyncResponseConsumerWrapperTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private AsyncResponseConsumer<String> consumer;

    @Mock
    private HttpResponse response;

    @Mock
    private EntityDetails entityDetails;

    @Before
    public void setUp() {
        ServiceManager.INSTANCE.boot();
    }

    @Test
    public void responseWithoutEntityFinishesSpan() throws Exception {
        when(response.getCode()).thenReturn(200);

        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);
        AsyncExitSpan exitSpan = createAsyncExitSpan("/no-body");

        AsyncResponseConsumerWrapper<String> wrapper =
                new AsyncResponseConsumerWrapper<>(consumer, exitSpan);

        HttpContext context = new BasicHttpContext();

        wrapper.consumeResponse(response, null, context, (FutureCallback<String>) null);

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));

        List<AbstractTracingSpan> spans =
                SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));

        assertThat(spans.size(), is(2));

        AbstractTracingSpan responseSpan = findSpan(spans, "/no-body");
        assertThat(responseSpan, notNullValue());
        assertThat(SpanHelper.getErrorOccurred(responseSpan), is(false));

        verify(consumer).consumeResponse(response, null, context, null);
    }

    @Test
    public void releaseBeforeFailureMarksSpanAsError() throws Exception {
        when(response.getCode()).thenReturn(200);

        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);
        AsyncExitSpan exitSpan = createAsyncExitSpan("/body-failure");

        AsyncResponseConsumerWrapper<String> wrapper =
                new AsyncResponseConsumerWrapper<>(consumer, exitSpan);

        HttpContext context = new BasicHttpContext();
        RuntimeException cause = new RuntimeException("body read failed");

        // Response headers were received successfully, but the body will fail.
        wrapper.consumeResponse(
                response,
                entityDetails,
                context,
                (FutureCallback<String>) null
        );

        // releaseResources() can happen before failed(). The span must
        // therefore be finished as an error rather than as a success.
        wrapper.releaseResources();

        // The later failure callback must not modify an already finished span.
        wrapper.failed(cause);

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));

        List<AbstractTracingSpan> spans =
                SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));

        assertThat(spans.size(), is(2));

        AbstractTracingSpan responseSpan = findSpan(spans, "/body-failure");
        assertThat(responseSpan, notNullValue());
        assertThat(SpanHelper.getErrorOccurred(responseSpan), is(true));

        verify(consumer).consumeResponse(
                response,
                entityDetails,
                context,
                null
        );
        verify(consumer).releaseResources();
        verify(consumer).failed(cause);
    }

    @Test
    public void streamEndFinishesResponseSpan() throws Exception {
        when(response.getCode()).thenReturn(200);

        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);
        AsyncExitSpan exitSpan = createAsyncExitSpan("/body");

        AsyncResponseConsumerWrapper<String> wrapper =
                new AsyncResponseConsumerWrapper<>(consumer, exitSpan);

        HttpContext context = new BasicHttpContext();

        wrapper.consumeResponse(
                response,
                entityDetails,
                context,
                (FutureCallback<String>) null
        );

        wrapper.streamEnd(null);

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));

        List<AbstractTracingSpan> spans =
                SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));

        assertThat(spans.size(), is(2));

        AbstractTracingSpan responseSpan = findSpan(spans, "/body");
        assertThat(responseSpan, notNullValue());
        assertThat(SpanHelper.getErrorOccurred(responseSpan), is(false));

        verify(consumer).streamEnd(null);
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

    private AbstractTracingSpan findSpan(
            List<AbstractTracingSpan> spans,
            String operationName) {
        for (AbstractTracingSpan span : spans) {
            if (operationName.equals(span.getOperationName())) {
                return span;
            }
        }
        return null;
    }
}