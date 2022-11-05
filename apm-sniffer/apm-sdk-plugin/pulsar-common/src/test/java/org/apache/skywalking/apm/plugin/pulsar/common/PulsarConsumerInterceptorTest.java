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

package org.apache.skywalking.apm.plugin.pulsar.common;

import java.util.List;
import org.apache.pulsar.client.impl.LookupService;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.pulsar.common.api.proto.PulsarApi;
import org.apache.skywalking.apm.agent.core.context.SW8CarrierItem;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SegmentRefHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.PULSAR_CONSUMER;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class PulsarConsumerInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private MockMessage msg;

    private EnhancedInstance consumerInstance = new EnhancedInstance() {
        @Override
        public Object getSkyWalkingDynamicField() {
            return "my-sub";
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {

        }
    };

    @Before
    public void setUp() {
        msg = new MockMessage();
        msg.getMessageBuilder()
                .addProperties(PulsarApi.KeyValue.newBuilder()
                        .setKey(SW8CarrierItem.HEADER_NAME)
                        .setValue("1-My40LjU=-MS4yLjM=-3-c2VydmljZQ==-aW5zdGFuY2U=-L2FwcA==-MTI3LjAuMC4xOjgwODA="));
    }

    @Test
    public void testConsumerWithNullMessage() throws Throwable {
        PulsarConsumerInterceptor consumerInterceptor = new PulsarConsumerInterceptor();
        consumerInterceptor.beforeMethod(consumerInstance, null, new Object[] {null}, new Class[0], null);
        consumerInterceptor.afterMethod(consumerInstance, null, new Object[] {null}, new Class[0], null);

        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(0));
    }

    @Test
    public void testConsumerWithMessage() throws Throwable {
        EnhancedInstance enhancedInstance = mockConsumer();
        PulsarConsumerInterceptor consumerInterceptor = new PulsarConsumerInterceptor();
        consumerInterceptor.beforeMethod(enhancedInstance, null, new Object[] {msg}, new Class[0], null);
        consumerInterceptor.afterMethod(enhancedInstance, null, new Object[] {msg}, new Class[0], null);

        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(1));

        TraceSegment traceSegment = traceSegments.get(0);
        assertNotNull(traceSegment.getRef());
        assertTraceSegmentRef(traceSegment.getRef());

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertConsumerSpan(spans.get(0));
    }

    @Test
    public void testConsumerWithMessageListener() throws Throwable {
        EnhancedInstance enhancedInstance = mockConsumer();
        PulsarConsumerInterceptor consumerInterceptor = new PulsarConsumerInterceptor();
        consumerInterceptor.beforeMethod(enhancedInstance, null, new Object[]{msg}, new Class[0], null);
        consumerInterceptor.afterMethod(enhancedInstance, null, new Object[]{msg}, new Class[0], null);

        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(1));

        TraceSegment traceSegment = traceSegments.get(0);
        assertNotNull(traceSegment.getRef());
        assertTraceSegmentRef(traceSegment.getRef());

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertConsumerSpan(spans.get(0));

        final MessageEnhanceRequiredInfo requiredInfo = (MessageEnhanceRequiredInfo) msg.getSkyWalkingDynamicField();
        assertNotNull(requiredInfo.getContextSnapshot());
    }

    private void assertConsumerSpan(AbstractTracingSpan span) {
        SpanAssert.assertLayer(span, SpanLayer.MQ);
        SpanAssert.assertComponent(span, PULSAR_CONSUMER);
        SpanAssert.assertTagSize(span, 2);
        SpanAssert.assertTag(span, 0, "pulsar://localhost:6650");
        SpanAssert.assertTag(span, 1, "persistent://my-tenant/my-ns/my-topic");
    }

    private void assertTraceSegmentRef(TraceSegmentRef ref) {
        MatcherAssert.assertThat(SegmentRefHelper.getParentServiceInstance(ref), is("instance"));
        MatcherAssert.assertThat(SegmentRefHelper.getSpanId(ref), is(3));
        MatcherAssert.assertThat(SegmentRefHelper.getTraceSegmentId(ref).toString(), is("3.4.5"));
    }

    private EnhancedInstance mockConsumer() throws Throwable {
        EnhancedInstance pulsarProducerInstance = PowerMockito.mock(MockConsumerImpl.class);
        final LookupService lookup = PowerMockito.mock(LookupService.class);
        final PulsarClientImpl client = PowerMockito.mock(PulsarClientImpl.class);
        PowerMockito.when(lookup, "getServiceUrl").thenReturn("pulsar://localhost:6650");
        PowerMockito.when(client, "getLookup").thenReturn(lookup);
        PowerMockito.when(pulsarProducerInstance, "getClient").thenReturn(client);
        PowerMockito.when(pulsarProducerInstance, "getTopic").thenReturn("persistent://my-tenant/my-ns/my-topic");
        PowerMockito.when(pulsarProducerInstance, "getSkyWalkingDynamicField").thenReturn("my-sub");
        return pulsarProducerInstance;
    }
}
