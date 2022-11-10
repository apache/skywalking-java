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
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.impl.LookupService;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.pulsar.common.api.proto.PulsarApi;
import org.apache.skywalking.apm.agent.core.context.SW8CarrierItem;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef.SegmentRefType;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class PulsarConsumerListenerInterceptorTest {
    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    private final EnhancedInstance consumerConfigurationDataInstance = new EnhancedInstance() {
        @Override
        public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
        }
    };
    private PulsarConsumerListenerInterceptor consumerListenerInterceptor;
    private MockMessage msg;
    private MockConsumer consumer;
    private MessageListener messageListener;

    @Before
    public void setUp() {
        consumerListenerInterceptor = new PulsarConsumerListenerInterceptor();
        messageListener = (consumer, message) -> message.getTopicName();
        msg = new MockMessage();
        msg.getMessageBuilder()
                .addProperties(PulsarApi.KeyValue.newBuilder()
                        .setKey(SW8CarrierItem.HEADER_NAME)
                        .setValue("1-My40LjU=-MS4yLjM=-3-c2VydmljZQ==-aW5zdGFuY2U=-L2FwcA==-MTI3LjAuMC4xOjgwODA="));
        msg.setSkyWalkingDynamicField(new MessageEnhanceRequiredInfo());
        consumer = new MockConsumer();
    }

    @Test
    public void testWithNoMessageListener() throws Throwable {
        consumerListenerInterceptor
                .beforeMethod(consumerConfigurationDataInstance, null, new Object[0], new Class[0], null);
        final MessageListener messageListener = (MessageListener) consumerListenerInterceptor
                .afterMethod(consumerConfigurationDataInstance, null, new Object[0], new Class[0], null);

        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(0));
        assertNull(messageListener);
    }

    @Test
    public void testWithMessageListenerHasNoRequiredInfo() throws Throwable {
        consumerListenerInterceptor
                .beforeMethod(consumerConfigurationDataInstance, null, new Object[0], new Class[0], null);
        final MessageListener enhancedMessageListener = (MessageListener) consumerListenerInterceptor
                .afterMethod(consumerConfigurationDataInstance, null, new Object[0], new Class[0],
                        this.messageListener);
        assertNotNull(enhancedMessageListener);
        msg.setSkyWalkingDynamicField(null);
        enhancedMessageListener.received(consumer, msg);

        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(0));
    }

    @Test
    public void testWithMessageListenerHasRequiredInfo() throws Throwable {
        PulsarConsumerInterceptor consumerInterceptor = new PulsarConsumerInterceptor();
        EnhancedInstance consumer =  mockConsumer();
        consumerInterceptor.beforeMethod(consumer, null, new Object[]{msg}, new Class[0], null);
        consumerInterceptor.afterMethod(consumer, null, new Object[]{msg}, new Class[0], null);
        consumerListenerInterceptor
                .beforeMethod(consumerConfigurationDataInstance, null, new Object[0], new Class[0], null);
        final MessageListener enhancedMessageListener = (MessageListener) consumerListenerInterceptor
                .afterMethod(consumerConfigurationDataInstance, null, new Object[0], new Class[0],
                        this.messageListener);
        assertNotNull(enhancedMessageListener);
        enhancedMessageListener.received((Consumer) consumer, msg);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(2));

        TraceSegment traceSegment = traceSegments.get(1);
        assertNotNull(traceSegment.getRef());
        assertTraceSegmentRef(traceSegment.getRef());

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertConsumerSpan(spans.get(0));
    }

    private EnhancedInstance mockConsumer() {
        EnhancedInstance pulsarProducerInstance = PowerMockito.mock(MockConsumerImpl.class);
        final LookupService lookup = PowerMockito.mock(LookupService.class);
        final PulsarClientImpl client = PowerMockito.mock(PulsarClientImpl.class);
        try {
            PowerMockito.when(lookup, "getServiceUrl").thenReturn("pulsar://localhost:6650");
            PowerMockito.when(client, "getLookup").thenReturn(lookup);
            PowerMockito.when(pulsarProducerInstance, "getClient").thenReturn(client);
            PowerMockito.when(pulsarProducerInstance, "getTopic").thenReturn("persistent://my-tenant/my-ns/my-topic");
            PowerMockito.when(pulsarProducerInstance, "getSkyWalkingDynamicField").thenReturn("my-sub");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return pulsarProducerInstance;
    }

    private void assertConsumerSpan(AbstractTracingSpan span) {
        SpanAssert.assertLayer(span, SpanLayer.MQ);
        SpanAssert.assertComponent(span, PULSAR_CONSUMER);
    }

    private void assertTraceSegmentRef(TraceSegmentRef ref) {
        MatcherAssert.assertThat(ref.getParentEndpoint(),
                is("Pulsar/persistent://my-tenant/my-ns/my-topic/Consumer/my-sub"));
        MatcherAssert.assertThat(SegmentRefHelper.getSpanId(ref), is(0));
        MatcherAssert.assertThat(SegmentRefHelper.getTraceSegmentId(ref), is("3.4.5"));
        MatcherAssert.assertThat(ref.getType(), is(SegmentRefType.CROSS_THREAD));
    }
}
