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

package org.apache.skywalking.apm.plugin.spring.kafka;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
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
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.kafka.define.Constants;
import org.apache.skywalking.apm.plugin.kafka.define.InterceptorMethod;
import org.apache.skywalking.apm.plugin.kafka.define.KafkaContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(TracingSegmentRunner.class)
public class ExtendedKafkaConsumerInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ExtendedKafkaConsumerInterceptor consumerInterceptor;

    private EnhancedInstance enhancedInstance;

    private ExtendedConsumerEnhanceRequiredInfo requiredInfo;

    private Method consumerMethod;

    @Before
    public void setUp() throws NoSuchMethodException {
        consumerInterceptor = new ExtendedKafkaConsumerInterceptor();
        enhancedInstance = new EnhancedInstance() {
            @Override
            public Object getSkyWalkingDynamicField() {
                return requiredInfo;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                requiredInfo = (ExtendedConsumerEnhanceRequiredInfo) value;
            }
        };

        requiredInfo = new ExtendedConsumerEnhanceRequiredInfo();
        requiredInfo.setGroupId("testGroup");
        requiredInfo.setBrokerServers("localhost:9092");
        consumerMethod = ConsumerRecords.class.getDeclaredMethod("records", TopicPartition.class);
    }

    @Test
    public void testBeforeMethod() throws Throwable {
        consumerInterceptor.beforeMethod(enhancedInstance, consumerMethod, new Object[0], new Class[0], null);
        assertThat(requiredInfo.getStartTime() > 0, is(true));
    }

    @Test
    public void testAfterMethodWithNullResult() throws Throwable {
        assertNull(
            consumerInterceptor.afterMethod(enhancedInstance, consumerMethod, new Object[0], new Class[0], null));
    }

    @Test
    public void testAfterMethodWithEmptyRecords() throws Throwable {
        Map<TopicPartition, List<ConsumerRecord>> records = new HashMap<>();
        ConsumerRecords consumerRecords = new ConsumerRecords(records);
        Object result = consumerInterceptor.afterMethod(
            enhancedInstance, consumerMethod, new Object[0], new Class[0], consumerRecords);
        assertThat(result, is(consumerRecords));
        assertThat(segmentStorage.getTraceSegments().size(), is(0));
    }

    @Test
    public void testAfterMethodWithRecords() throws Throwable {
        // Prepare Kafka context
        KafkaContext kafkaContext = new KafkaContext("/spring-kafka/pollAndInvoke");
        kafkaContext.setNeedStop(true);
        ContextManager.getRuntimeContext().put(Constants.KAFKA_FLAG, kafkaContext);

        // Prepare consumer records - create ConsumerRecords
        TopicPartition topicPartition = new TopicPartition("test", 1);
        List<ConsumerRecord<Object, Object>> recordList = new ArrayList<>();
        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader("sw8", new byte[0]));
        ConsumerRecord<Object, Object> consumerRecord = new ConsumerRecord<>(
            "test", 1, 0, 0L, null, null, 0, 0, "test", "test", headers);
        recordList.add(consumerRecord);

        // create ConsumerRecords
        Map<TopicPartition, List<ConsumerRecord<Object, Object>>> recordsMap = new HashMap<>();
        recordsMap.put(topicPartition, recordList);
        ConsumerRecords<Object, Object> consumerRecords = new ConsumerRecords<>(recordsMap);

        // Execute interceptor
        Object result = consumerInterceptor.afterMethod(
            enhancedInstance, consumerMethod, new Object[0], new Class[0], consumerRecords);
        InterceptorMethod.endKafkaPollAndInvokeIteration(result);
        assertThat(result, is(consumerRecords));

        // Verify trace segment
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(1));
        TraceSegment traceSegment = traceSegments.get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        AbstractTracingSpan span = spans.get(0);
        SpanAssert.assertLayer(span, SpanLayer.MQ);
        SpanAssert.assertComponent(span, ComponentsDefine.KAFKA_CONSUMER);
        assertThat(span.getOperationName(), is("Kafka/test/Consumer/testGroup"));
    }

    @Test
    public void testOnConstructWithEmptyArguments() throws Throwable {
        ExtendedConstructorInterceptPoint constructorInterceptPoint = new ExtendedConstructorInterceptPoint();
        constructorInterceptPoint.onConstruct(enhancedInstance, new Object[] {});
        assertThat(requiredInfo.getGroupId(), is("Unknown"));
        assertThat(requiredInfo.getBrokerServers(), is("Unknown"));
    }

    @Test
    public void testOnConstructWithMapConfig() throws Throwable {
        ExtendedConstructorInterceptPoint constructorInterceptPoint = new ExtendedConstructorInterceptPoint();
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("group.id", "testGroup");
        constructorInterceptPoint.onConstruct(enhancedInstance, new Object[] {config});
        assertThat(requiredInfo.getGroupId(), is("testGroup"));
        assertThat(requiredInfo.getBrokerServers(), is("localhost:9092"));
    }

    @Test
    public void testHandleMethodException() throws Throwable {
        consumerInterceptor.beforeMethod(enhancedInstance, consumerMethod, new Object[0], new Class[0], null);
        consumerInterceptor.handleMethodException(
            enhancedInstance, consumerMethod, new Object[0], new Class[0], new RuntimeException("test exception"));
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(0));
    }
}