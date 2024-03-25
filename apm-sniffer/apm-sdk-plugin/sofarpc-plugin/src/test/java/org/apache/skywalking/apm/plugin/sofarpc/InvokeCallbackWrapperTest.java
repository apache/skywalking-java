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

package org.apache.skywalking.apm.plugin.sofarpc;

import com.alipay.remoting.InvokeCallback;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(TracingSegmentRunner.class)
public class InvokeCallbackWrapperTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    private Executor executor = Executors.newFixedThreadPool(1);

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private InvokeCallback callback;

    @Before
    public void before() {
        callback = new InvokeCallback() {
            @Override
            public void onResponse(final Object o) {
            }

            @Override
            public void onException(final Throwable throwable) {
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        };
    }

    static class WrapperWrapper implements InvokeCallback {

        private InvokeCallback callback;

        private CountDownLatch countDownLatch;

        public CountDownLatch getCountDownLatch() {
            return countDownLatch;
        }

        public WrapperWrapper(InvokeCallback callback) {
            this.countDownLatch = new CountDownLatch(1);
            this.callback = callback;
        }

        @Override
        public void onResponse(final Object o) {
            callback.onResponse(o);
            countDownLatch.countDown();
        }

        @Override
        public void onException(final Throwable throwable) {
            callback.onException(throwable);
            countDownLatch.countDown();
        }

        @Override
        public Executor getExecutor() {
            return null;
        }
    }

    @Test
    public void testConstruct() {
        InvokeCallbackWrapper wrapper = new InvokeCallbackWrapper(callback);
        Assert.assertSame(callback, wrapper.getInvokeCallback());
        Assert.assertNull(wrapper.getContextSnapshot());

        ContextManager.createEntrySpan("sofarpc", null);
        wrapper = new InvokeCallbackWrapper(callback);
        Assert.assertSame(callback, wrapper.getInvokeCallback());
        Assert.assertEquals(ContextManager.getGlobalTraceId(), wrapper.getContextSnapshot().getTraceId().getId());
        Assert.assertEquals("sofarpc", wrapper.getContextSnapshot().getParentEndpoint());
        ContextManager.stopSpan();
    }

    @Test
    public void testOnResponse() throws InterruptedException {
        ContextManager.createEntrySpan("sofarpc", null);
        InvokeCallbackWrapper wrapper = new InvokeCallbackWrapper(callback);
        final WrapperWrapper wrapperWrapper = new WrapperWrapper(wrapper);
        executor.execute(() -> wrapperWrapper.onResponse(null));
        ContextManager.stopSpan();
        wrapperWrapper.getCountDownLatch().await();

        assertThat(segmentStorage.getTraceSegments().size(), is(2));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        TraceSegment traceSegment2 = segmentStorage.getTraceSegments().get(1);
        List<AbstractTracingSpan> spans2 = SegmentHelper.getSpans(traceSegment2);
        assertThat(spans2.size(), is(1));
        assertEquals("sofarpc", traceSegment2.getRef().getParentEndpoint());
    }

    @Test
    public void testOnException() throws InterruptedException {
        ContextManager.createEntrySpan("sofarpc", null);
        InvokeCallbackWrapper wrapper = new InvokeCallbackWrapper(callback);
        final WrapperWrapper wrapperWrapper = new WrapperWrapper(wrapper);
        final Throwable throwable = new Throwable();
        executor.execute(() -> wrapperWrapper.onException(throwable));
        ContextManager.stopSpan();
        wrapperWrapper.getCountDownLatch().await();

        assertThat(segmentStorage.getTraceSegments().size(), is(2));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        TraceSegment traceSegment2 = segmentStorage.getTraceSegments().get(1);
        List<AbstractTracingSpan> spans2 = SegmentHelper.getSpans(traceSegment2);
        assertThat(spans2.size(), is(1));
        assertThat(SpanHelper.getLogs(spans2.get(0)).size(), is(1));

    }

}