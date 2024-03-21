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
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(TracingSegmentRunner.class)
public class SofaBoltCallbackInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private EnhancedInstance enhancedInstance = new EnhancedInstance() {

        private Object object;

        @Override
        public Object getSkyWalkingDynamicField() {
            return object;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.object = value;
        }
    };

    @Mock
    private ContextSnapshot contextSnapshot;
    private final Executor executor = Executors.newFixedThreadPool(1);

    private SofaBoltCallbackConstructInterceptor constructInterceptor;
    private SofaBoltCallbackExceptionInterceptor exceptionInterceptor;
    private SofaBoltCallbackInvokeInterceptor invokeInterceptor;

    private Object[] arguments;
    private Object[] throwableArgs;

    private Method responseMethod;
    private Method exceptionMethod;
    private InvokeCallback callback;

    @Before
    public void before() throws NoSuchMethodException {
        constructInterceptor = new SofaBoltCallbackConstructInterceptor();
        exceptionInterceptor = new SofaBoltCallbackExceptionInterceptor();
        invokeInterceptor = new SofaBoltCallbackInvokeInterceptor();
        callback = new InvokeCallback() {

            @Override
            public void onResponse(Object o) {
                ContextManager.createLocalSpan("onResponse");
                ContextManager.stopSpan();
            }

            @Override
            public void onException(Throwable throwable) {

            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        };

        responseMethod = callback.getClass().getMethod("onResponse", Object.class);
        exceptionMethod = callback.getClass().getMethod("onException", Throwable.class);
        arguments = new Object[] {new Object()};
        throwableArgs = new Object[] {new NullPointerException()};
    }

    @Test
    public void testOnConstructor() {
        constructInterceptor.onConstruct(enhancedInstance, null);
        Assert.assertNull(enhancedInstance.getSkyWalkingDynamicField());
    }

    @Test
    public void testResponse() throws Throwable {
        enhancedInstance.setSkyWalkingDynamicField(contextSnapshot);
        invokeInterceptor.beforeMethod(enhancedInstance, responseMethod, arguments, null, null);
        invokeInterceptor.afterMethod(enhancedInstance, responseMethod, arguments, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
    }

    @Test
    public void testException() throws Throwable {
        enhancedInstance.setSkyWalkingDynamicField(contextSnapshot);
        exceptionInterceptor.beforeMethod(enhancedInstance, exceptionMethod, throwableArgs, null, null);
        exceptionInterceptor.afterMethod(enhancedInstance, exceptionMethod, throwableArgs, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
    }

    @Test
    public void testResponseIntegrally() throws InterruptedException {
        AbstractSpan testBegin = ContextManager.createLocalSpan("TestBegin");
        constructInterceptor.onConstruct(enhancedInstance, null);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ContextManager.stopSpan(testBegin);

        executor.execute(() -> {
            invokeInterceptor.beforeMethod(enhancedInstance, responseMethod, arguments, null, null);
            callback.onResponse(new Object());
            invokeInterceptor.afterMethod(enhancedInstance, responseMethod, arguments, null, null);
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(2, segmentStorage.getTraceSegments().size());
        TraceSegment traceSegment1 = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans1 = SegmentHelper.getSpans(traceSegment1);
        assertEquals(1, spans1.size());
        TraceSegment traceSegment2 = segmentStorage.getTraceSegments().get(1);
        List<AbstractTracingSpan> spans2 = SegmentHelper.getSpans(traceSegment2);
        assertEquals(2, spans2.size());
        assertEquals("onResponse", spans2.get(0).getOperationName());

        assertEquals(traceSegment1.getRelatedGlobalTrace().getId(), traceSegment2.getRef().getTraceId());
    }

}
