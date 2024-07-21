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

package org.apache.skywalking.apm.plugin.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Kafka37ConsumerInterceptor extends KafkaConsumerInterceptor {

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (ret instanceof ConsumerRecords) {
            ConsumerEnhanceRequiredInfo requiredInfo = (ConsumerEnhanceRequiredInfo) objInst.getSkyWalkingDynamicField();
            ConsumerRecords<?, ?> consumerRecords = (ConsumerRecords<?, ?>) ret;
            if (consumerRecords.count() == 0) {
                return ret;
            }
            for (ConsumerRecord<?, ?> consumerRecord : consumerRecords) {
                if (consumerRecord == null) {
                    continue;
                }
                ContextCarrier contextCarrier = new ContextCarrier();
                CarrierItem next = contextCarrier.items();
                while (next.hasNext()) {
                    next = next.next();
                    Iterator<Header> iterator = consumerRecord.headers().headers(next.getHeadKey()).iterator();
                    if (iterator.hasNext()) {
                        next.setHeadValue(new String(iterator.next().value(), StandardCharsets.UTF_8));
                    }
                }
                String operationName = OPERATE_NAME_PREFIX + requiredInfo.getTopics() + CONSUMER_OPERATE_NAME + requiredInfo.getGroupId();
                AbstractSpan activeSpan = ContextManager.createEntrySpan(operationName, contextCarrier).start(requiredInfo.getStartTime());
                activeSpan.setComponent(ComponentsDefine.KAFKA_CONSUMER);
                SpanLayer.asMQ(activeSpan);
                Tags.MQ_BROKER.set(activeSpan, requiredInfo.getBrokerServers());
                Tags.MQ_TOPIC.set(activeSpan, requiredInfo.getTopics());
                activeSpan.setPeer(requiredInfo.getBrokerServers());
                ContextManager.stopSpan();
            }
        }
        return ret;
    }
}
