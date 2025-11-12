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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.kafka.define.Constants;
import org.apache.skywalking.apm.plugin.kafka.define.KafkaContext;

public class ExtendedKafkaConsumerInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String OPERATE_NAME_PREFIX = "Kafka/";
    private static final String CONSUMER_OPERATE_NAME = "/Consumer/";
    private static final String UNKNOWN = "Unknown";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        ExtendedConsumerEnhanceRequiredInfo requiredInfo = (ExtendedConsumerEnhanceRequiredInfo) objInst.getSkyWalkingDynamicField();
        if (requiredInfo != null) {
            requiredInfo.setStartTime(System.currentTimeMillis());
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        if (ret == null) {
            return ret;
        }

        ConsumerRecords<?, ?> records = (ConsumerRecords<?, ?>) ret;

        // Only create entry span when consumer received at least one message
        if (records.count() > 0) {
            createEntrySpan(objInst, records);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }

    private void createEntrySpan(EnhancedInstance objInst, ConsumerRecords<?, ?> records) {
        KafkaContext context = (KafkaContext) ContextManager.getRuntimeContext().get(Constants.KAFKA_FLAG);
        if (context != null) {
            ContextManager.createEntrySpan(context.getOperationName(), null);
            context.setNeedStop(true);
        }

        ExtendedConsumerEnhanceRequiredInfo requiredInfo = (ExtendedConsumerEnhanceRequiredInfo) objInst.getSkyWalkingDynamicField();

        SpanInfo spanInfo = buildSpanInfo(requiredInfo, records);

        String operationName = OPERATE_NAME_PREFIX + spanInfo.topic + CONSUMER_OPERATE_NAME + spanInfo.groupId;
        AbstractSpan activeSpan = ContextManager.createEntrySpan(operationName, null);

        if (requiredInfo != null) {
            activeSpan.start(requiredInfo.getStartTime());
        }

        activeSpan.setComponent(ComponentsDefine.KAFKA_CONSUMER);
        SpanLayer.asMQ(activeSpan);
        Tags.MQ_BROKER.set(activeSpan, spanInfo.brokerServers);
        Tags.MQ_TOPIC.set(activeSpan, spanInfo.topic);
        activeSpan.setPeer(spanInfo.brokerServers);

        extractContextCarrier(records);
        ContextManager.stopSpan();
    }

    private SpanInfo buildSpanInfo(ExtendedConsumerEnhanceRequiredInfo requiredInfo, ConsumerRecords<?, ?> records) {
        String topic = UNKNOWN;
        String groupId = UNKNOWN;
        String brokerServers = UNKNOWN;

        if (requiredInfo != null) {
            groupId = requiredInfo.getGroupId();
            brokerServers = requiredInfo.getBrokerServers();
        }

        Set<TopicPartition> partitions = records.partitions();
        if (!partitions.isEmpty()) {
            topic = partitions.stream()
                              .map(TopicPartition::topic).distinct()
                              .collect(Collectors.joining(";"));
        }

        return new SpanInfo(topic, groupId, brokerServers);
    }

    private void extractContextCarrier(ConsumerRecords<?, ?> records) {
        for (ConsumerRecord<?, ?> record : records) {
            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();

            while (next.hasNext()) {
                next = next.next();
                Iterator<Header> iterator = record.headers().headers(next.getHeadKey()).iterator();
                if (iterator.hasNext()) {
                    next.setHeadValue(new String(iterator.next().value(), StandardCharsets.UTF_8));
                }
            }
            ContextManager.extract(contextCarrier);
        }
    }

    private static class SpanInfo {
        final String topic;
        final String groupId;
        final String brokerServers;

        SpanInfo(String topic, String groupId, String brokerServers) {
            this.topic = topic;
            this.groupId = groupId;
            this.brokerServers = brokerServers;
        }
    }
}