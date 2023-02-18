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
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.toolkit.trace.CarrierItemRef;
import org.apache.skywalking.apm.toolkit.trace.ContextCarrierRef;
import org.apache.skywalking.apm.toolkit.trace.Tracer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class ContextCarrierRefTest {
    @SegmentStoragePoint
    private SegmentStorage storage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EnhancedInstance enhancedContextCarrierRef;

    @Mock
    private EnhancedInstance enhancedCarrierItemRef;

    @Mock
    private EnhancedInstance enhancedSpanRef;

    private ContextCarrierRefItemsInterceptor contextCarrierRefItemsInterceptor;

    private CarrierItemRefNextInterceptor carrierItemRefNextInterceptor;

    private CarrierItemRefHasNextInterceptor carrierItemRefHasNextInterceptor;

    private CarrierItemGetHeadKeyInterceptor carrierItemGetHeadKeyInterceptor;

    private CarrierItemGetHeadValueInterceptor carrierItemGetHeadValueInterceptor;

    private CarrierItemSetHeadValueInterceptor carrierItemSetHeadValueInterceptor;

    private TracerCreateExitSpanWithContextInterceptor tracerCreateExitSpanWithContextInterceptor;

    private TracerStopSpanInterceptor tracerStopSpanInterceptor;

    @Before
    public void setup() throws Exception {
        contextCarrierRefItemsInterceptor = new ContextCarrierRefItemsInterceptor();
        carrierItemRefNextInterceptor = new CarrierItemRefNextInterceptor();
        carrierItemRefHasNextInterceptor = new CarrierItemRefHasNextInterceptor();
        carrierItemGetHeadKeyInterceptor = new CarrierItemGetHeadKeyInterceptor();
        carrierItemGetHeadValueInterceptor = new CarrierItemGetHeadValueInterceptor();
        carrierItemSetHeadValueInterceptor = new CarrierItemSetHeadValueInterceptor();
        tracerCreateExitSpanWithContextInterceptor = new TracerCreateExitSpanWithContextInterceptor();
        tracerStopSpanInterceptor = new TracerStopSpanInterceptor();

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

        enhancedCarrierItemRef = new EnhancedInstance() {
            CarrierItem carrierItem;

            @Override
            public Object getSkyWalkingDynamicField() {
                return this.carrierItem;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.carrierItem = (CarrierItem) value;
            }
        };

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
    public void testCarrierMethods() throws Throwable {
        Method items = ContextCarrierRef.class.getDeclaredMethod("items");
        Method createExitSpan = Tracer.class.getDeclaredMethod("createExitSpan", String.class, ContextCarrierRef.class, String.class);
        Method stopSpan = Tracer.class.getDeclaredMethod("stopSpan");
        Method hasNext = CarrierItemRef.class.getDeclaredMethod("hasNext");
        Method next = CarrierItemRef.class.getDeclaredMethod("next");
        Method getHeadKey = CarrierItemRef.class.getDeclaredMethod("getHeadKey");
        Method getHeadValue = CarrierItemRef.class.getDeclaredMethod("getHeadValue");
        Method setHeadValue = CarrierItemRef.class.getDeclaredMethod("setHeadValue", String.class);
        String headKey;
        String headValue;
        Boolean ifHasNext = true;

        tracerCreateExitSpanWithContextInterceptor.beforeMethod(Tracer.class, createExitSpan, null, null, null);
        tracerCreateExitSpanWithContextInterceptor.afterMethod(Tracer.class, createExitSpan, new Object[]{"testExitSpanWithContext", enhancedContextCarrierRef, "127.0.0.1:8888"}, null, enhancedSpanRef);
        contextCarrierRefItemsInterceptor.beforeMethod(enhancedContextCarrierRef, items, null, null, null);
        enhancedCarrierItemRef = (EnhancedInstance) contextCarrierRefItemsInterceptor.afterMethod(enhancedContextCarrierRef, items, null, null, enhancedCarrierItemRef);
        tracerStopSpanInterceptor.beforeMethod(Tracer.class, stopSpan, null, null, null);
        tracerStopSpanInterceptor.afterMethod(Tracer.class, stopSpan, null, null, null);

        assertThat(storage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = storage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        ContextCarrier contextCarrierFromRef = (ContextCarrier) enhancedContextCarrierRef.getSkyWalkingDynamicField();
        CarrierItem carrierItem = contextCarrierFromRef.items();

        carrierItemSetHeadValueInterceptor.beforeMethod(enhancedCarrierItemRef, setHeadValue, new Object[]{"hello"}, null, null);
        carrierItemSetHeadValueInterceptor.afterMethod(enhancedCarrierItemRef, setHeadValue, null, null, null);
        carrierItemGetHeadValueInterceptor.beforeMethod(enhancedCarrierItemRef, getHeadValue, null, null, null);
        headValue = (String) carrierItemGetHeadValueInterceptor.afterMethod(enhancedCarrierItemRef, getHeadValue, null, null, null);
        assertThat(headValue, is("hello"));

        while (ifHasNext) {
            carrierItemRefHasNextInterceptor.beforeMethod(enhancedCarrierItemRef, hasNext, null, null, null);
            ifHasNext = (Boolean) carrierItemRefHasNextInterceptor.afterMethod(enhancedCarrierItemRef, hasNext, null, null, null);
            assertThat(carrierItem.hasNext(), is(ifHasNext));

            if (!ifHasNext) break;

            carrierItemRefNextInterceptor.beforeMethod(enhancedCarrierItemRef, next, null, null, null);
            enhancedCarrierItemRef = (EnhancedInstance) carrierItemRefNextInterceptor.afterMethod(enhancedCarrierItemRef, next, null, null, enhancedCarrierItemRef);
            carrierItemGetHeadKeyInterceptor.beforeMethod(enhancedCarrierItemRef, getHeadKey, null, null, null);
            headKey = (String) carrierItemGetHeadKeyInterceptor.afterMethod(enhancedCarrierItemRef, getHeadKey, null, null, null);
            carrierItemGetHeadValueInterceptor.beforeMethod(enhancedCarrierItemRef, getHeadValue, null, null, null);
            headValue = (String) carrierItemGetHeadValueInterceptor.afterMethod(enhancedCarrierItemRef, getHeadValue, null, null, null);
            carrierItem = carrierItem.next();

            assertThat(headKey, is(carrierItem.getHeadKey()));
            assertThat(headValue, is(carrierItem.getHeadValue()));
        }
    }
}
