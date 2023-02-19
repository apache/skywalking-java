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
import java.util.List;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.EntrySpan;
import org.apache.skywalking.apm.agent.core.context.trace.ExitSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LocalSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.toolkit.trace.ContextCarrierRef;
import org.apache.skywalking.apm.toolkit.trace.ContextSnapshotRef;
import org.apache.skywalking.apm.toolkit.trace.Tracer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class TracerTest {
    @SegmentStoragePoint
    private SegmentStorage storage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EnhancedInstance enhancedSpanRef;

    @Mock
    private EnhancedInstance enhancedContextCarrierRef;

    @Mock
    private EnhancedInstance enhancedContextSnapshotRef;

    private TracerCaptureInterceptor tracerCaptureInterceptor;

    private TracerContinuedInterceptor tracerContinuedInterceptor;

    private TracerCreateEntrySpanInterceptor tracerCreateEntrySpanInterceptor;

    private TracerCreateLocalSpanInterceptor tracerCreateLocalSpanInterceptor;

    private TracerCreateExitSpanNoContextInterceptor tracerCreateExitSpanNoContextInterceptor;

    private TracerCreateExitSpanWithContextInterceptor tracerCreateExitSpanWithContextInterceptor;

    private TracerExtractInterceptor tracerExtractInterceptor;

    private TracerInjectInterceptor tracerInjectInterceptor;

    private TracerStopSpanInterceptor tracerStopSpanInterceptor;

    @Before
    public void setUp() throws Exception {
        tracerCaptureInterceptor = new TracerCaptureInterceptor();
        tracerContinuedInterceptor = new TracerContinuedInterceptor();
        tracerCreateEntrySpanInterceptor = new TracerCreateEntrySpanInterceptor();
        tracerCreateLocalSpanInterceptor = new TracerCreateLocalSpanInterceptor();
        tracerCreateExitSpanNoContextInterceptor = new TracerCreateExitSpanNoContextInterceptor();
        tracerCreateExitSpanWithContextInterceptor = new TracerCreateExitSpanWithContextInterceptor();
        tracerExtractInterceptor = new TracerExtractInterceptor();
        tracerInjectInterceptor = new TracerInjectInterceptor();
        tracerStopSpanInterceptor = new TracerStopSpanInterceptor();

        enhancedSpanRef = new EnhancedInstance() {
            AbstractTracingSpan abstractTracingSpan;

            @Override
            public Object getSkyWalkingDynamicField() {
                return this.abstractTracingSpan;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.abstractTracingSpan = (AbstractTracingSpan) value;
            }
        };

        enhancedContextCarrierRef = new EnhancedInstance() {
            ContextCarrier contextCarrier = new ContextCarrier();

            @Override
            public Object getSkyWalkingDynamicField() {
                return this.contextCarrier;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.contextCarrier = (ContextCarrier) value;
            }
        };

        enhancedContextSnapshotRef = new EnhancedInstance() {
            ContextSnapshot contextSnapshot;

            @Override
            public Object getSkyWalkingDynamicField() {
                return this.contextSnapshot;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.contextSnapshot = (ContextSnapshot) value;
            }
        };
    }

    @Test
    public void testTracerCreateEntrySpan() throws Throwable {
        Method createEntrySpan = Tracer.class.getDeclaredMethod("createEntrySpan", String.class, ContextCarrierRef.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");

        tracerCreateEntrySpanInterceptor.beforeMethod(Tracer.class, createEntrySpan, null, null, null);
        EnhancedInstance entrySpanRef = (EnhancedInstance) tracerCreateEntrySpanInterceptor.afterMethod(Tracer.class, createEntrySpan, new Object[]{"createEntrySpanMethod", null}, null, enhancedSpanRef);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        AbstractTracingSpan tracingSpan = spans.get(0);
        SpanAssert.assertLogSize(tracingSpan, 0);
        SpanAssert.assertTagSize(tracingSpan, 0);
        assertThat(tracingSpan.getOperationName(), is("createEntrySpanMethod"));

        EntrySpan entrySpanFromRef = (EntrySpan) entrySpanRef.getSkyWalkingDynamicField();
        SpanAssert.assertLogSize(entrySpanFromRef, 0);
        SpanAssert.assertTagSize(entrySpanFromRef, 0);
        assertThat(entrySpanFromRef.getOperationName(), is("createEntrySpanMethod"));
    }

    @Test
    public void testTracerCreateLocalSpan() throws Throwable {
        Method createLocalSpan = Tracer.class.getDeclaredMethod("createLocalSpan", String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");
        tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
        EnhancedInstance localSpanRef = (EnhancedInstance) tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[]{"testCreateLocalSpanMethod"}, null, enhancedSpanRef);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        AbstractTracingSpan tracingSpan = spans.get(0);
        SpanAssert.assertLogSize(tracingSpan, 0);
        SpanAssert.assertTagSize(tracingSpan, 0);
        assertThat(tracingSpan.getOperationName(), is("testCreateLocalSpanMethod"));

        AbstractTracingSpan localSpanFromRef = (AbstractTracingSpan) localSpanRef.getSkyWalkingDynamicField();
        SpanAssert.assertLogSize(localSpanFromRef, 0);
        SpanAssert.assertTagSize(localSpanFromRef, 0);
        assertThat(localSpanFromRef.getOperationName(), is("testCreateLocalSpanMethod"));
    }

    @Test
    public void testTracerInjectAndExtract() throws Throwable {
        Method createExitSpan = Tracer.class.getDeclaredMethod("createExitSpan", String.class, String.class);
        Method createLocalSpan = Tracer.class.getDeclaredMethod("createLocalSpan", String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");
        Method inject = Tracer.class.getDeclaredMethod("inject", ContextCarrierRef.class);
        Method extract = Tracer.class.getDeclaredMethod("extract", ContextCarrierRef.class);

        Config.Agent.SERVICE_NAME = "testTracerCreateExitSpanNoContextService";
        Config.Agent.INSTANCE_NAME = "testTracerCreateExitSpanNoContextIns";

        tracerCreateExitSpanNoContextInterceptor.beforeMethod(Tracer.class, createExitSpan, null, null, null);
        EnhancedInstance exitSpanRef = (EnhancedInstance) tracerCreateExitSpanNoContextInterceptor.afterMethod(Tracer.class, createExitSpan, new Object[]{"testCreateExitSpanMethod", "127.0.0.1:6666"}, null, enhancedSpanRef);
        tracerInjectInterceptor.beforeMethod(Tracer.class, inject, null, null, null);
        tracerInjectInterceptor.afterMethod(Tracer.class, inject, new Object[] {enhancedContextCarrierRef}, null, null);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        ExitSpan exitSpan = (ExitSpan) spans.get(0);
        SpanAssert.assertLogSize(exitSpan, 0);
        SpanAssert.assertTagSize(exitSpan, 0);
        assertThat(exitSpan.getOperationName(), is("testCreateExitSpanMethod"));
        assertThat(exitSpan.getPeer(), is("127.0.0.1:6666"));

        ExitSpan exitSpanFromRef = (ExitSpan) exitSpanRef.getSkyWalkingDynamicField();
        SpanAssert.assertLogSize(exitSpanFromRef, 0);
        SpanAssert.assertTagSize(exitSpanFromRef, 0);
        assertThat(exitSpanFromRef.getOperationName(), is("testCreateExitSpanMethod"));
        assertThat(exitSpanFromRef.getPeer(), is("127.0.0.1:6666"));

        ContextCarrier contextCarrierFromInject = (ContextCarrier) (enhancedContextCarrierRef.getSkyWalkingDynamicField());
        Assert.assertNotNull(contextCarrierFromInject);
        assertThat(contextCarrierFromInject.getSpanId(), is(exitSpan.getSpanId()));
        assertThat(contextCarrierFromInject.getTraceSegmentId(), is(traceSegment.getTraceSegmentId()));
        assertThat(contextCarrierFromInject.getTraceId(), is(traceSegment.getRelatedGlobalTrace().getId()));
        assertThat(contextCarrierFromInject.getAddressUsedAtClient(), is(exitSpan.getPeer()));

        Thread thread = new Thread(() -> {
            try {
                tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
                tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[] {"newSegLocalSpan"}, null, enhancedSpanRef);
                tracerExtractInterceptor.beforeMethod(Tracer.class, extract, null, null, null);
                tracerExtractInterceptor.afterMethod(Tracer.class, extract, new Object[] {enhancedContextCarrierRef}, null, null);
                tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
                tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

                assertThat(storage.getTraceSegments().size(), is(2));
                TraceSegment newTraceSegment = storage.getTraceSegments().get(1);
                assertThat(newTraceSegment.getRef().getTraceId(), is(traceSegment.getRelatedGlobalTrace().getId()));
                assertThat(newTraceSegment.getRef().getTraceSegmentId(), is(traceSegment.getTraceSegmentId()));
                assertThat(newTraceSegment.getRelatedGlobalTrace().getId(), is(traceSegment.getRelatedGlobalTrace().getId()));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        thread.start();
        thread.join();
    }

    @Test
    public void testTracerCreateExitSpanWithContext() throws Throwable {
        Method createExitSpan = Tracer.class.getDeclaredMethod("createExitSpan", String.class, ContextCarrierRef.class, String.class);
        Method createEntrySpan = Tracer.class.getDeclaredMethod("createEntrySpan", String.class, ContextCarrierRef.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");

        Config.Agent.SERVICE_NAME = "testTracerCreateExitSpanNoContextService";
        Config.Agent.INSTANCE_NAME = "testTracerCreateExitSpanNoContextIns";

        tracerCreateExitSpanWithContextInterceptor.beforeMethod(Tracer.class, createExitSpan, null, null, null);
        EnhancedInstance exitSpanRef = (EnhancedInstance) tracerCreateExitSpanWithContextInterceptor.afterMethod(Tracer.class, createExitSpan, new Object[]{"testExitSpanWithContext", enhancedContextCarrierRef, "127.0.0.1:5555"}, null, enhancedSpanRef);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        ExitSpan exitSpan = (ExitSpan) spans.get(0);
        SpanAssert.assertLogSize(exitSpan, 0);
        SpanAssert.assertTagSize(exitSpan, 0);
        assertThat(exitSpan.getOperationName(), is("testExitSpanWithContext"));
        assertThat(exitSpan.getPeer(), is("127.0.0.1:5555"));

        ExitSpan exitSpanFromRef = (ExitSpan) exitSpanRef.getSkyWalkingDynamicField();
        SpanAssert.assertLogSize(exitSpanFromRef, 0);
        SpanAssert.assertTagSize(exitSpanFromRef, 0);
        assertThat(exitSpanFromRef.getOperationName(), is("testExitSpanWithContext"));
        assertThat(exitSpanFromRef.getPeer(), is("127.0.0.1:5555"));

        ContextCarrier contextCarrierFromInject = (ContextCarrier) (((EnhancedInstance) enhancedContextCarrierRef).getSkyWalkingDynamicField());
        Assert.assertNotNull(contextCarrierFromInject);
        assertThat(contextCarrierFromInject.getSpanId(), is(exitSpan.getSpanId()));
        assertThat(contextCarrierFromInject.getTraceSegmentId(), is(traceSegment.getTraceSegmentId()));
        assertThat(contextCarrierFromInject.getTraceId(), is(traceSegment.getRelatedGlobalTrace().getId()));
        assertThat(contextCarrierFromInject.getAddressUsedAtClient(), is(exitSpan.getPeer()));

        Thread thread = new Thread(() -> {
            try {
                enhancedSpanRef.setSkyWalkingDynamicField(null);
                tracerCreateEntrySpanInterceptor.beforeMethod(Tracer.class, createEntrySpan, null, null, null);
                EnhancedInstance entrySpanRef = (EnhancedInstance) tracerCreateEntrySpanInterceptor.afterMethod(Tracer.class, createEntrySpan, new Object[] {"entrySpanFromExtract", enhancedContextCarrierRef}, null, enhancedSpanRef);
                tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
                tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

                assertThat(storage.getTraceSegments().size(), is(2));
                TraceSegment newTraceSegment = storage.getTraceSegments().get(1);
                assertThat(newTraceSegment.getRef().getTraceId(), is(traceSegment.getRelatedGlobalTrace().getId()));
                assertThat(newTraceSegment.getRef().getTraceSegmentId(), is(traceSegment.getTraceSegmentId()));
                assertThat(newTraceSegment.getRelatedGlobalTrace().getId(), is(traceSegment.getRelatedGlobalTrace().getId()));

                List<AbstractTracingSpan> spans1 = SegmentHelper.getSpans(newTraceSegment);
                assertThat(spans1.size(), is(1));
                EntrySpan entrySpan = (EntrySpan) spans1.get(0);
                assertThat(entrySpan.isEntry(), is(true));
                assertThat(entrySpan.getOperationName(), is("entrySpanFromExtract"));

                EntrySpan entrySpanFromRef = (EntrySpan) entrySpanRef.getSkyWalkingDynamicField();
                SpanAssert.assertLogSize(entrySpanFromRef, 0);
                SpanAssert.assertTagSize(entrySpanFromRef, 0);
                assertThat(entrySpanFromRef.isEntry(), is(true));
                assertThat(entrySpanFromRef.getOperationName(), is("entrySpanFromExtract"));
                assertThat(entrySpanFromRef.getSpanId(), is(entrySpan.getSpanId()));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        thread.start();
        thread.join();
    }

    @Test
    public void testTracerCaptureAndContinued() throws Throwable {
        Method capture = Tracer.class.getDeclaredMethod("capture");
        Method continued = Tracer.class.getDeclaredMethod("continued", ContextSnapshotRef.class);
        Method createLocalSpan = Tracer.class.getDeclaredMethod("createLocalSpan", String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");

        tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
        tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[] {"localSpanForCapture"}, null, enhancedSpanRef);
        tracerCaptureInterceptor.beforeMethod(Tracer.class, capture, null, null, null);
        EnhancedInstance enhancedInsSnapshot = (EnhancedInstance) tracerCaptureInterceptor.afterMethod(Tracer.class, capture, null, null, enhancedContextSnapshotRef);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        Thread thread = new Thread(() -> {
            try {
                tracerCreateLocalSpanInterceptor.beforeMethod(Tracer.class, createLocalSpan, null, null, null);
                tracerCreateLocalSpanInterceptor.afterMethod(Tracer.class, createLocalSpan, new Object[] {"localSpanForContinued"}, null, enhancedSpanRef);
                tracerContinuedInterceptor.beforeMethod(Tracer.class, continued, new Object[] {enhancedInsSnapshot}, null, null);
                tracerContinuedInterceptor.afterMethod(Tracer.class, continued, null, null, null);
                tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
                tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

                assertThat(storage.getTraceSegments().size(), is(2));
                TraceSegment newTraceSegment = storage.getTraceSegments().get(1);
                assertThat(newTraceSegment.getRef().getTraceId(), is(traceSegment.getRelatedGlobalTrace().getId()));
                assertThat(newTraceSegment.getRef().getTraceSegmentId(), is(traceSegment.getTraceSegmentId()));
                assertThat(newTraceSegment.getRelatedGlobalTrace().getId(), is(traceSegment.getRelatedGlobalTrace().getId()));

                List<AbstractTracingSpan> newSpans = SegmentHelper.getSpans(newTraceSegment);
                LocalSpan newSpan = (LocalSpan) newSpans.get(0);
                assertThat(newSpan.getOperationName(), is("localSpanForContinued"));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        thread.start();
        thread.join();
    }
}
