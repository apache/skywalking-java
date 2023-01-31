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

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.PULSAR_PRODUCER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import org.apache.pulsar.client.impl.LookupService;
import org.apache.pulsar.client.impl.MessageImpl;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TracingSegmentRunner.class)
public class PulsarProducerInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private PulsarProducerInterceptor producerInterceptor;

    private Object[] arguments;
    private Class[] argumentType;

    private MessageImpl msg = new MockMessage();

    @Before
    public void setUp() {
        producerInterceptor = new PulsarProducerInterceptor();
        arguments = new Object[] {
            msg,
            null
        };
        argumentType = new Class[] {MessageImpl.class};
    }

    @Test
    public void testSendMessage() throws Throwable {

        MockProducerImpl pulsarProducerInstance = mock(MockProducerImpl.class);
        final LookupService lookup = mock(LookupService.class);
        final PulsarClientImpl client = mock(PulsarClientImpl.class);
        when(lookup.getServiceUrl()).thenReturn("pulsar://localhost:6650");
        when(client.getLookup()).thenReturn(lookup);
        when(pulsarProducerInstance.getClient()).thenReturn(client);
        when(pulsarProducerInstance.getTopic()).thenReturn("persistent://my-tenant/my-ns/my-topic");
        when(pulsarProducerInstance.getSkyWalkingDynamicField()).thenReturn((MessagePropertiesInjector) (message, carrierItem) -> {
            });
        producerInterceptor.beforeMethod(pulsarProducerInstance, null, arguments, argumentType, null);
        producerInterceptor.afterMethod(pulsarProducerInstance, null, arguments, argumentType, null);

        List<TraceSegment> traceSegmentList = segmentStorage.getTraceSegments();
        assertThat(traceSegmentList.size(), is(1));

        TraceSegment segment = traceSegmentList.get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segment);
        assertThat(spans.size(), is(1));

        assertMessageSpan(spans.get(0));
    }

    @Test
    public void testSendWithNullMessage() throws Throwable {
        EnhancedInstance pulsarProducerInstance = mock(MockProducerImpl.class);
        when(pulsarProducerInstance.getSkyWalkingDynamicField()).thenReturn((MessagePropertiesInjector) (message, carrierItem) -> {
        });
        producerInterceptor.beforeMethod(pulsarProducerInstance, null, new Object[] {null}, argumentType, null);
        producerInterceptor.afterMethod(pulsarProducerInstance, null, new Object[] {null}, argumentType, null);
        List<TraceSegment> traceSegmentList = segmentStorage.getTraceSegments();
        assertThat(traceSegmentList.size(), is(0));
    }

    private void assertMessageSpan(AbstractTracingSpan span) {
        SpanAssert.assertTag(span, 0, "pulsar://localhost:6650");
        SpanAssert.assertTag(span, 1, "persistent://my-tenant/my-ns/my-topic");
        SpanAssert.assertComponent(span, PULSAR_PRODUCER);
        SpanAssert.assertLayer(span, SpanLayer.MQ);
        assertThat(span.getOperationName(), is("Pulsar/persistent://my-tenant/my-ns/my-topic/Producer"));
    }
}
