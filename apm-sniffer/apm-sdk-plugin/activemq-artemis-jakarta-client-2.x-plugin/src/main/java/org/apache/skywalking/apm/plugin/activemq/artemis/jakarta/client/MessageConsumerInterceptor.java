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

package org.apache.skywalking.apm.plugin.activemq.artemis.jakarta.client;

import java.lang.reflect.Method;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.jms.client.ActiveMQMessage;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.activemq.artemis.jakarta.client.define.EnhanceInfo;

/**
 * {@link MessageConsumerInterceptor} create entry span when the method {@link org.apache.activemq.artemis.jms.client.ActiveMQMessageConsumer#getMessage(long, boolean)} execute
 */
public class MessageConsumerInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String OPERATION_NAME_PREFIX = "ActiveMQ/";
    private static final String CONSUMER_OPERATION_NAME_SUFFIX = "/Consumer";
    public static final StringTag MQ_MESSAGE_ID = new StringTag("mq.message.id");
    private static final String QUEUE = "Queue";
    private static final String TOPIC = "Topic";

    @Override
    public void beforeMethod(final EnhancedInstance objInst,
                             final Method method,
                             final Object[] allArguments,
                             final Class<?>[] classes,
                             final MethodInterceptResult methodInterceptResult) throws Throwable {
    }

    @Override
    public Object afterMethod(final EnhancedInstance objInst,
                              final Method method,
                              final Object[] objects,
                              final Class<?>[] classes,
                              final Object ret) throws Throwable {
        ActiveMQMessage message = (ActiveMQMessage) ret;
        if (message == null) {
            return ret;
        }
        ContextCarrier contextCarrier = getContextCarrierFromMessage(message);
        EnhanceInfo enhanceInfo = (EnhanceInfo) objInst.getSkyWalkingDynamicField();
        boolean queue = isQueue(enhanceInfo.getType());
        AbstractSpan activeSpan = ContextManager.createEntrySpan(
            buildOperationName(queue, enhanceInfo.getName()),
            contextCarrier
        );
        Tags.MQ_BROKER.set(activeSpan, enhanceInfo.getBrokerUrl());
        if (queue) {
            Tags.MQ_QUEUE.set(activeSpan, enhanceInfo.getName());
        } else {
            Tags.MQ_TOPIC.set(activeSpan, enhanceInfo.getName());
        }
        activeSpan.tag(MQ_MESSAGE_ID, message.getJMSMessageID());
        activeSpan.setPeer(enhanceInfo.getBrokerUrl());
        activeSpan.setComponent(ComponentsDefine.ACTIVEMQ_CONSUMER);
        SpanLayer.asMQ(activeSpan);
        ContextManager.stopSpan(activeSpan);
        return ret;
    }

    @Override
    public void handleMethodException(final EnhancedInstance enhancedInstance,
                                      final Method method,
                                      final Object[] objects,
                                      final Class<?>[] classes,
                                      final Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private ContextCarrier getContextCarrierFromMessage(ActiveMQMessage message) {
        ContextCarrier contextCarrier = new ContextCarrier();

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(message.getCoreMessage().getStringProperty(next.getHeadKey().replace("-", "_")));
        }

        return contextCarrier;
    }

    private boolean isQueue(ActiveMQDestination.TYPE type) {
        return ActiveMQDestination.TYPE.QUEUE.equals(type) || ActiveMQDestination.TYPE.TEMP_QUEUE.equals(type);
    }

    private String buildOperationName(boolean isQueue, String name) {
        if (isQueue) {
            return OPERATION_NAME_PREFIX + QUEUE + "/" + name + CONSUMER_OPERATION_NAME_SUFFIX;
        } else {
            return OPERATION_NAME_PREFIX + TOPIC + "/" + name + CONSUMER_OPERATION_NAME_SUFFIX;
        }
    }
}
