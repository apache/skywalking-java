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

package org.apache.skywalking.apm.plugin.spring.rabbitmq;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class SpringRabbitMQConsumerInterceptor implements InstanceMethodsAroundInterceptorV2 {
    public static final String OPERATE_NAME_PREFIX = "RabbitMQ/";
    public static final String CONSUMER_OPERATE_NAME_SUFFIX = "/Consumer";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInvocationContext context) throws Throwable {
        Channel channel = (Channel) allArguments[0];

        if (allArguments[1] instanceof Message) {
            // Single message consume
            Message message = (Message) allArguments[1];
            MessageProperties messageProperties = message.getMessageProperties();
            Map<String, Object> headers = messageProperties.getHeaders();

            ContextCarrier contextCarrier = buildContextCarrier(headers);
            String operationName = buildOperationName(messageProperties);
            AbstractSpan activeSpan = ContextManager.createEntrySpan(operationName, contextCarrier);

            setSpanAttributes(activeSpan, channel, messageProperties);
        } else if (allArguments[1] instanceof List) {
            // Batch message consume
            List<?> messages = (List<?>) allArguments[1];
            if (messages.isEmpty()) {
                return;
            }

            // Use the first message to create EntrySpan
            Message firstMessage = (Message) messages.get(0);
            MessageProperties firstMessageProperties = firstMessage.getMessageProperties();
            Map<String, Object> firstMessageHeaders = firstMessageProperties.getHeaders();

            ContextCarrier contextCarrier = buildContextCarrier(firstMessageHeaders);
            String operationName = buildOperationName(firstMessageProperties);
            AbstractSpan activeSpan = ContextManager.createEntrySpan(operationName, contextCarrier);

            setSpanAttributes(activeSpan, channel, firstMessageProperties);

            // Extract trace context from remaining messages (skip first, already used for EntrySpan)
            // to correlate all producer traces with this consumer span
            for (int i = 1; i < messages.size(); i++) {
                Object msg = messages.get(i);
                if (msg instanceof Message) {
                    Message message = (Message) msg;
                    MessageProperties messageProperties = message.getMessageProperties();
                    Map<String, Object> headers = messageProperties.getHeaders();

                    ContextCarrier carrier = buildContextCarrier(headers);
                    if (carrier.isValid()) {
                        ContextManager.extract(carrier);
                    }
                }
            }
        }
    }

    /**
     * Build ContextCarrier from message headers
     */
    private ContextCarrier buildContextCarrier(Map<String, Object> headers) {
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            Object value = headers.get(next.getHeadKey());
            if (value != null) {
                next.setHeadValue(value.toString());
            }
        }
        return contextCarrier;
    }

    private String buildOperationName(MessageProperties messageProperties) {
        return OPERATE_NAME_PREFIX + "Topic/" + messageProperties.getReceivedExchange()
               + "Queue/" + messageProperties.getReceivedRoutingKey()
               + CONSUMER_OPERATE_NAME_SUFFIX;
    }

    private void setSpanAttributes(AbstractSpan span, Channel channel, MessageProperties messageProperties) {
        Connection connection = channel.getConnection();
        String serverUrl = connection.getAddress().getHostAddress() + ":" + connection.getPort();
        Tags.MQ_BROKER.set(span, serverUrl);
        Tags.MQ_TOPIC.set(span, messageProperties.getReceivedExchange());
        Tags.MQ_QUEUE.set(span, messageProperties.getReceivedRoutingKey());
        span.setComponent(ComponentsDefine.RABBITMQ_CONSUMER);
        span.setPeer(serverUrl);
        SpanLayer.asMQ(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret, MethodInvocationContext context) throws Throwable {
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }
}
