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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.wrapper.SwCallableWrapper;
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

    private final ThreadPoolInvokeMethodInterceptor interceptor = new ThreadPoolInvokeMethodInterceptor();

    @Test
    public void shouldWrapCallablesInOrderWithoutMutatingOriginalCollection() throws Throwable {
        Callable<String> first = new StringCallable("first");
        Callable<String> second = new StringCallable("second");
        List<Callable<String>> original = Collections.unmodifiableList(Arrays.asList(first, second));
        Object[] arguments = new Object[] {original};
        MethodInvocationContext context = new MethodInvocationContext();
        ContextManager.createEntrySpan("parent", new ContextCarrier());

        interceptor.beforeMethod(enhancedInstance, invokeAllMethod(), arguments, null, context);

        assertThat(arguments[0] == original, is(false));
        assertThat(arguments[0], instanceOf(List.class));
        List<?> wrapped = (List<?>) arguments[0];
        assertThat(wrapped.size(), is(2));
        assertThat(wrapped.get(0), instanceOf(SwCallableWrapper.class));
        assertThat(wrapped.get(1), instanceOf(SwCallableWrapper.class));
        assertThat(original.get(0), sameInstance(first));
        assertThat(original.get(1), sameInstance(second));

        interceptor.afterMethod(enhancedInstance, invokeAllMethod(), arguments, null, null, context);
        assertThat(ContextManager.isActive(), is(true));
        ContextManager.stopSpan();

        assertInvocationSpan("ThreadPoolExecutor/invokeAll", false);
    }

    @Test
    public void shouldSkipCallablesThatAlreadyCarryTracingContext() throws Throwable {
        ContextManager.createEntrySpan("parent", new ContextCarrier());
        Callable<String> delegate = new StringCallable("wrapped");
        SwCallableWrapper alreadyWrapped = new SwCallableWrapper(delegate, ContextManager.capture());
        CapturedCallable capturedCallable = new CapturedCallable(ContextManager.capture());
        Object[] arguments = new Object[] {Arrays.asList(alreadyWrapped, capturedCallable)};
        MethodInvocationContext context = new MethodInvocationContext();

        interceptor.beforeMethod(enhancedInstance, invokeAnyMethod(), arguments, null, context);

        List<?> wrapped = (List<?>) arguments[0];
        assertThat(wrapped.get(0), sameInstance((Object) alreadyWrapped));
        assertThat(wrapped.get(1), sameInstance((Object) capturedCallable));

        interceptor.afterMethod(enhancedInstance, invokeAnyMethod(), arguments, null, null, context);
        ContextManager.stopSpan();

        assertInvocationSpan("ThreadPoolExecutor/invokeAny", false);
    }

    @Test
    public void shouldTraceAllInvokeOverloads() throws Throwable {
        Method[] methods = new Method[] {
            invokeAllMethod(), timedInvokeAllMethod(), invokeAnyMethod(), timedInvokeAnyMethod()
        };
        String[] operationNames = new String[] {
            "ThreadPoolExecutor/invokeAll", "ThreadPoolExecutor/invokeAll",
            "ThreadPoolExecutor/invokeAny", "ThreadPoolExecutor/invokeAny"
        };

        for (Method method : methods) {
            Object[] arguments = method.getParameterTypes().length == 1
                ? new Object[] {Collections.emptyList()}
                : new Object[] {Collections.emptyList(), 1L, TimeUnit.SECONDS};
            MethodInvocationContext context = new MethodInvocationContext();
            ContextManager.createEntrySpan("parent", new ContextCarrier());

            interceptor.beforeMethod(enhancedInstance, method, arguments, method.getParameterTypes(), context);
            interceptor.afterMethod(enhancedInstance, method, arguments, method.getParameterTypes(), null, context);
            ContextManager.stopSpan();
        }

        assertThat(segmentStorage.getTraceSegments().size(), is(4));
        for (int i = 0; i < operationNames.length; i++) {
            assertInvocationSpan(segmentStorage.getTraceSegments().get(i), operationNames[i], false);
        }
    }

    @Test
    public void shouldIgnoreUnexpectedArgumentsWithoutStoppingParentSpan() throws Throwable {
        Object[][] unexpectedArguments = new Object[][] {
            null,
            new Object[0],
            new Object[] {null},
            new Object[] {"not-a-collection"}
        };

        for (Object[] arguments : unexpectedArguments) {
            MethodInvocationContext context = new MethodInvocationContext();
            ContextManager.createEntrySpan("parent", new ContextCarrier());

            interceptor.beforeMethod(enhancedInstance, invokeAllMethod(), arguments, null, context);
            interceptor.handleMethodException(
                enhancedInstance, invokeAllMethod(), arguments, null, new IllegalStateException("ignored"), context);
            interceptor.afterMethod(enhancedInstance, invokeAllMethod(), arguments, null, null, context);

            assertThat(context.getContext(), is((Object) null));
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
    public void shouldLogExceptionOnInvocationSpan() throws Throwable {
        Object[] arguments = new Object[] {Collections.singletonList(new StringCallable("callable"))};
        MethodInvocationContext context = new MethodInvocationContext();
        ContextManager.createEntrySpan("parent", new ContextCarrier());

        interceptor.beforeMethod(enhancedInstance, invokeAnyMethod(), arguments, null, context);
        interceptor.handleMethodException(
            enhancedInstance, invokeAnyMethod(), arguments, null, new IllegalStateException("test"), context);
        interceptor.afterMethod(enhancedInstance, invokeAnyMethod(), arguments, null, null, context);

        assertThat(ContextManager.isActive(), is(true));
        ContextManager.stopSpan();

        assertInvocationSpan("ThreadPoolExecutor/invokeAny", true);
    }

    private void assertInvocationSpan(String operationName, boolean hasException) {
        assertInvocationSpan(segmentStorage.getTraceSegments().get(0), operationName, hasException);
    }

    private void assertInvocationSpan(TraceSegment traceSegment, String operationName, boolean hasException) {
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(2));
        assertThat(spans.get(0).getOperationName(), is(operationName));
        SpanAssert.assertOccurException(spans.get(0), hasException);
        assertThat(spans.get(1).getOperationName(), is("parent"));
    }

    private Method invokeAllMethod() throws NoSuchMethodException {
        return AbstractExecutorService.class.getMethod("invokeAll", Collection.class);
    }

    private Method timedInvokeAllMethod() throws NoSuchMethodException {
        return AbstractExecutorService.class.getMethod(
            "invokeAll", Collection.class, long.class, TimeUnit.class);
    }

    private Method invokeAnyMethod() throws NoSuchMethodException {
        return AbstractExecutorService.class.getMethod("invokeAny", Collection.class);
    }

    private Method timedInvokeAnyMethod() throws NoSuchMethodException {
        return AbstractExecutorService.class.getMethod(
            "invokeAny", Collection.class, long.class, TimeUnit.class);
    }

    private static class StringCallable implements Callable<String> {
        private final String value;

        private StringCallable(String value) {
            this.value = value;
        }

        @Override
        public String call() {
            return value;
        }
    }

    private static class CapturedCallable implements Callable<String>, EnhancedInstance {
        private Object dynamicField;

        private CapturedCallable(ContextSnapshot contextSnapshot) {
            this.dynamicField = contextSnapshot;
        }

        @Override
        public String call() {
            return "captured";
        }

        @Override
        public Object getSkyWalkingDynamicField() {
            return dynamicField;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.dynamicField = value;
        }
    }
}
