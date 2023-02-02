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

package org.apache.skywalking.apm.toolkit.activation.trace;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * build an EnhancedInstance to store the created span
 * and then use this span to do
 *     1. prepareForAsync
 *     2. asyncFinish
 *     3. log(Throwable)
 *     4. log(Map)
 */

import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LocalSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.toolkit.trace.SpanRef;
import org.apache.skywalking.apm.toolkit.trace.Tracer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class SpanRefTest {
    @SegmentStoragePoint
    private SegmentStorage storage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EnhancedInstance enhancedSpanRef;

    private SpanRefPrepareForAsyncInterceptor spanRefPrepareForAsyncInterceptor;

    private SpanRefAsyncFinishInterceptor spanRefAsyncFinishInterceptor;

    private SpanRefLogMapInterceptor spanRefLogMapInterceptor;

    private SpanRefLogThrowableInterceptor spanRefLogThrowableInterceptor;

    private SpanRefTagInterceptor spanRefTagInterceptor;

    private TracerCreateLocalSpanInterceptor tracerCreateLocalSpanInterceptor;

    private TracerStopSpanInterceptor tracerStopSpanInterceptor;

    @Before
    public void setUp() throws Exception {
        spanRefPrepareForAsyncInterceptor = new SpanRefPrepareForAsyncInterceptor();
        spanRefAsyncFinishInterceptor = new SpanRefAsyncFinishInterceptor();
        spanRefLogMapInterceptor = new SpanRefLogMapInterceptor();
        spanRefLogThrowableInterceptor = new SpanRefLogThrowableInterceptor();
        spanRefTagInterceptor = new SpanRefTagInterceptor();
        tracerCreateLocalSpanInterceptor = new TracerCreateLocalSpanInterceptor();
        tracerStopSpanInterceptor = new TracerStopSpanInterceptor();

        enhancedSpanRef = new EnhancedInstance() {
            AbstractSpan span;

            @Override
            public Object getSkyWalkingDynamicField() {
                return this.span;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.span = (AbstractSpan) value;
            }
        };
    }

    @Test
    public void testSpanRefPrepareAndFinishForAsync() throws Throwable {
        Method prepareForAsync = SpanRef.class.getDeclaredMethod("prepareForAsync");
        Method asyncFinish = SpanRef.class.getDeclaredMethod("asyncFinish");
        Method createLocalSpan = Tracer.class.getDeclaredMethod("createLocalSpan", String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");

        tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
        enhancedSpanRef = (EnhancedInstance) tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[]{"localSpanForAsync"}, null, enhancedSpanRef);
        spanRefPrepareForAsyncInterceptor.beforeMethod(enhancedSpanRef, prepareForAsync, null, null, null);
        enhancedSpanRef = (EnhancedInstance) spanRefPrepareForAsyncInterceptor.afterMethod(enhancedSpanRef, prepareForAsync, null, null, enhancedSpanRef);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        assertThat(storage.getTraceSegments().size(), is(0));

        spanRefAsyncFinishInterceptor.beforeMethod(enhancedSpanRef, asyncFinish, null, null, null);
        enhancedSpanRef = (EnhancedInstance) spanRefAsyncFinishInterceptor.afterMethod(enhancedSpanRef, asyncFinish, null, null, enhancedSpanRef);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        AbstractTracingSpan tracingSpan = spans.get(0);
        assertThat(tracingSpan.getOperationName(), is("localSpanForAsync"));
    }

    @Test
    public void testSpanRefLogMap() throws Throwable {
        Method logForMap = SpanRef.class.getDeclaredMethod("log", Map.class);
        Method createLocalSpan = Tracer.class.getDeclaredMethod("createLocalSpan", String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");

        String eventInfo = "this log is for testing spanRef log function with Map parameter";
        Map<String, String> testEvents = new HashMap<>();
        testEvents.put("event", "info");
        testEvents.put("message", eventInfo);

        tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
        enhancedSpanRef = (EnhancedInstance) tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[]{"localSpanForLogMap"}, null, enhancedSpanRef);
        spanRefLogMapInterceptor.beforeMethod(enhancedSpanRef, logForMap, new Object[]{testEvents}, null, null);
        spanRefLogMapInterceptor.afterMethod(enhancedSpanRef, logForMap, null, null, null);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        LocalSpan localSpan = (LocalSpan) enhancedSpanRef.getSkyWalkingDynamicField();
        SpanAssert.assertLogSize(localSpan, 1);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        AbstractTracingSpan span = spans.get(0);
        SpanAssert.assertLogSize(span, 1);
    }

    @Test
    public void testSpanRefLogThrowable() throws Throwable {
        Method logForThrowable = SpanRef.class.getDeclaredMethod("log", Throwable.class);
        Method createLocalSpan = Tracer.class.getDeclaredMethod("createLocalSpan", String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");

        tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
        enhancedSpanRef = (EnhancedInstance) tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[]{"localSpanForLogThrowable"}, null, enhancedSpanRef);
        spanRefLogThrowableInterceptor.beforeMethod(enhancedSpanRef, logForThrowable, new Object[]{new RuntimeException("test-Throwable")}, null, null);
        spanRefLogThrowableInterceptor.afterMethod(enhancedSpanRef, logForThrowable, null, null, null);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        LocalSpan localSpan = (LocalSpan) enhancedSpanRef.getSkyWalkingDynamicField();
        SpanAssert.assertLogSize(localSpan, 1);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        AbstractTracingSpan span = spans.get(0);
        SpanAssert.assertLogSize(span, 1);
    }

    @Test
    public void testSpanRefTag() throws Throwable {
        Method tag = SpanRef.class.getDeclaredMethod("tag", String.class, String.class);
        Method createLocalSpan = Tracer.class.getDeclaredMethod("createLocalSpan", String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");

        tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
        enhancedSpanRef = (EnhancedInstance) tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[]{"localSpanForTag"}, null, enhancedSpanRef);
        spanRefTagInterceptor.beforeMethod(enhancedSpanRef, tag, new Object[] {"skywalking_tag_key", "skywalking_tag_value"}, null, null);
        spanRefTagInterceptor.afterMethod(enhancedSpanRef, tag, null, null, null);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        LocalSpan localSpan = (LocalSpan) enhancedSpanRef.getSkyWalkingDynamicField();
        SpanAssert.assertTagSize(localSpan, 1);
        SpanAssert.assertTag(localSpan, 0, "skywalking_tag_value");

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        AbstractTracingSpan span = spans.get(0);
        SpanAssert.assertTagSize(span, 1);
        SpanAssert.assertTag(span, 0, "skywalking_tag_value");
    }
}
