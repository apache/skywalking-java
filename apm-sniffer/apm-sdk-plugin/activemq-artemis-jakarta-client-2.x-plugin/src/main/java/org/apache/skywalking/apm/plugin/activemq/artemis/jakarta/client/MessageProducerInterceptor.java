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

import jakarta.jms.Message;
import java.lang.reflect.Method;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.jms.client.ActiveMQSession;
import org.apache.activemq.artemis.jms.client.ConnectionFactoryOptions;
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
 * {@link MessageProducerInterceptor} create exit span when the method {@link org.apache.activemq.artemis.jms.client.ActiveMQMessageProducer#doSendx(
 * ActiveMQConnection connection, ClientProducer producer, ActiveMQDestination defaultDestination, ActiveMQSession
 * session, ConnectionFactoryOptions options)} execute
 */
public class MessageProducerInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String OPERATION_NAME_PREFIX = "ActiveMQ/";
    private static final String PRODUCER_OPERATION_NAME_SUFFIX = "/Producer";
    private static final StringTag MQ_MESSAGE_ID = new StringTag("mq.message.id");
    private static final String QUEUE = "Queue";
    private static final String TOPIC = "Topic";

    @Override
    public void beforeMethod(final EnhancedInstance objInst,
                             final Method method,
                             final Object[] allArguments,
                             final Class<?>[] classes,
                             final MethodInterceptResult methodInterceptResult) throws Throwable {
        ContextCarrier contextCarrier = new ContextCarrier();
        Message message = (Message) allArguments[1];
        EnhanceInfo enhanceInfo = (EnhanceInfo) objInst.getSkyWalkingDynamicField();
        boolean queue = isQueue(enhanceInfo.getType());
        AbstractSpan activeSpan = ContextManager.createExitSpan(
            buildOperationName(queue, enhanceInfo.getName()),
            contextCarrier, enhanceInfo.getBrokerUrl()
        );
        contextCarrier.extensionInjector().injectSendingTimestamp();
        Tags.MQ_BROKER.set(activeSpan, enhanceInfo.getBrokerUrl());
        if (queue) {
            Tags.MQ_QUEUE.set(activeSpan, enhanceInfo.getName());
        } else {
            Tags.MQ_TOPIC.set(activeSpan, enhanceInfo.getName());
        }
        SpanLayer.asMQ(activeSpan);
        activeSpan.setComponent(ComponentsDefine.ACTIVEMQ_PRODUCER);
        CarrierItem next = contextCarrier.items();

        while (next.hasNext()) {
            next = next.next();
            message.setStringProperty(next.getHeadKey().replace("-", "_"), next.getHeadValue());
        }
    }

    @Override
    public Object afterMethod(final EnhancedInstance enhancedInstance,
                              final Method method,
                              final Object[] allArguments,
                              final Class<?>[] classes,
                              final Object ret) throws Throwable {
        AbstractSpan activeSpan = ContextManager.activeSpan();
        Message message = (Message) allArguments[1];
        activeSpan.tag(MQ_MESSAGE_ID, message.getJMSMessageID());
        ContextManager.stopSpan();
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

    private boolean isQueue(ActiveMQDestination.TYPE type) {
        return ActiveMQDestination.TYPE.QUEUE.equals(type) || ActiveMQDestination.TYPE.TEMP_QUEUE.equals(type);
    }

    private String buildOperationName(boolean isQueue, String name) {
        if (isQueue) {
            return OPERATION_NAME_PREFIX + QUEUE + "/" + name + PRODUCER_OPERATION_NAME_SUFFIX;
        } else {
            return OPERATION_NAME_PREFIX + TOPIC + "/" + name + PRODUCER_OPERATION_NAME_SUFFIX;
        }
    }
}
