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

package org.apache.skywalking.apm.plugin;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(TracingSegmentRunner.class)
public class ThreadPoolInvokeMethodInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @Mock
    private EnhancedInstance enhancedInstance;

    @Mock
    private MethodInterceptResult result;

    private final ThreadPoolInvokeMethodInterceptor interceptor = new ThreadPoolInvokeMethodInterceptor();

    @Test
    public void shouldIgnoreUnexpectedArguments() throws Throwable {
        Object[][] unexpectedArguments = new Object[][] {
            null,
            new Object[0],
            new Object[] {null},
            new Object[] {"not-a-collection"}
        };

        for (Object[] arguments : unexpectedArguments) {
            ContextManager.createEntrySpan("parent", new ContextCarrier());

            interceptor.beforeMethod(enhancedInstance, invokeAllMethod(), arguments, null, result);
            interceptor.handleMethodException(
                enhancedInstance, invokeAllMethod(), arguments, null, new IllegalStateException("ignored"));
            interceptor.afterMethod(enhancedInstance, invokeAllMethod(), arguments, null, null);

            assertThat(ContextManager.isActive(), is(true));
            ContextManager.stopSpan();
        }

        assertThat(segmentStorage.getTraceSegments().size(), is(4));
        for (TraceSegment traceSegment : segmentStorage.getTraceSegments()) {
            List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
            assertThat(spans.size(), is(1));
            assertThat(spans.get(0).getOperationName(), is("parent"));
            SpanAssert.assertOccurException(spans.get(0), false);
        }
    }

    @Test
    public void shouldTraceEmptyCollection() throws Throwable {
        Object[] arguments = new Object[] {Collections.emptyList()};
        ContextManager.createEntrySpan("parent", new ContextCarrier());

        interceptor.beforeMethod(enhancedInstance, invokeAllMethod(), arguments, null, result);
        interceptor.afterMethod(enhancedInstance, invokeAllMethod(), arguments, null, null);
        ContextManager.stopSpan();

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));
        assertThat(spans.size(), is(2));
        assertThat(spans.get(0).getOperationName(), is("ThreadPoolExecutor/invokeAll"));
    }

    @Test
    public void shouldTraceAlternativeInvokeAllSignature() throws Throwable {
        Object[] arguments = new Object[] {Collections.emptyList(), "alternative"};
        ContextManager.createEntrySpan("parent", new ContextCarrier());

        interceptor.beforeMethod(enhancedInstance, alternativeInvokeAllMethod(), arguments, null, result);
        interceptor.afterMethod(enhancedInstance, alternativeInvokeAllMethod(), arguments, null, null);
        ContextManager.stopSpan();

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));
        assertThat(spans.size(), is(2));
        assertThat(spans.get(0).getOperationName(), is("ThreadPoolExecutor/invokeAll"));
    }

    private Method invokeAllMethod() throws NoSuchMethodException {
        return AbstractExecutorService.class.getMethod("invokeAll", Collection.class);
    }

    private Method alternativeInvokeAllMethod() throws NoSuchMethodException {
        return getClass().getDeclaredMethod("invokeAll", Collection.class, String.class);
    }

    private void invokeAll(Collection<?> callables, String alternative) {
    }
}
