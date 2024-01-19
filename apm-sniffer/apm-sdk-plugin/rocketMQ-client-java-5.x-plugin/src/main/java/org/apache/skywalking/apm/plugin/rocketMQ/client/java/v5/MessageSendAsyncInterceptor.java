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

package org.apache.skywalking.apm.plugin.rocketMQ.client.java.v5;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.java.impl.ClientImpl;
import org.apache.rocketmq.client.java.message.MessageBuilderImpl;
import org.apache.rocketmq.shaded.com.google.gson.Gson;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.util.CollectionUtil;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * {@link MessageSendAsyncInterceptor} create exit span when the method {@link org.apache.rocketmq.client.java.impl.producer.ProducerImpl#sendAsync(org.apache.rocketmq.client.apis.message.Message)}
 * execute
 */
public class MessageSendAsyncInterceptor implements InstanceMethodsAroundInterceptor {

    public static final String ASYNC_SEND_OPERATION_NAME_PREFIX = "RocketMQ/";
    public static final Gson GSON = new Gson();

    @Override
    public void beforeMethod(EnhancedInstance objInst,
                             Method method,
                             Object[] allArguments,
                             Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        Message message = (Message) allArguments[0];
        ClientImpl producerImpl = (ClientImpl) objInst;

        ContextCarrier contextCarrier = new ContextCarrier();
        String namingServiceAddress = producerImpl.getClientConfiguration().getEndpoints();
        AbstractSpan span = ContextManager.createExitSpan(
            buildOperationName(message.getTopic()), contextCarrier, namingServiceAddress);
        span.setComponent(ComponentsDefine.ROCKET_MQ_PRODUCER);
        Tags.MQ_BROKER.set(span, namingServiceAddress);
        Tags.MQ_TOPIC.set(span, message.getTopic());
        Collection<String> keys = message.getKeys();
        if (!CollectionUtil.isEmpty(keys)) {
            span.tag(Tags.ofKey("mq.message.keys"), String.join(",", keys));
        }
        Optional<String> tag = message.getTag();
        tag.ifPresent(s -> span.tag(Tags.ofKey("mq.message.tags"), s));

        contextCarrier.extensionInjector().injectSendingTimestamp();
        SpanLayer.asMQ(span);

        Map<String, String> properties = message.getProperties();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (!StringUtil.isEmpty(next.getHeadValue())) {
                properties.put(next.getHeadKey(), next.getHeadValue());
            }
        }

        MessageBuilder messageBuilder = new MessageBuilderImpl();
        messageBuilder.setTopic(message.getTopic());
        if (message.getTag().isPresent()) {
            messageBuilder.setTag(message.getTag().get());
        }
        messageBuilder.setKeys(message.getKeys().toArray(new String[0]));
        if (message.getMessageGroup().isPresent()) {
            messageBuilder.setMessageGroup(message.getMessageGroup().get());
        }

        byte[] body = new byte[message.getBody().limit()];
        message.getBody().get(body);
        messageBuilder.setBody(body);
        if (message.getDeliveryTimestamp().isPresent()) {
            messageBuilder.setDeliveryTimestamp(message.getDeliveryTimestamp().get());
        }

        properties.forEach(messageBuilder::addProperty);
        allArguments[0] = messageBuilder.build();
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst,
                              Method method,
                              Object[] allArguments,
                              Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        CompletableFuture<SendReceipt> future = (CompletableFuture<SendReceipt>) ret;
        AbstractSpan span = ContextManager.activeSpan();
        span.prepareForAsync();
        ContextManager.stopSpan();
        return future.whenCompleteAsync((sendReceipt, throwable) -> {
            if (null != throwable) {
                span.log(throwable);
                span.errorOccurred();
                span.asyncFinish();
                return;
            }
            if (sendReceipt == null || sendReceipt.getMessageId() == null) {
                span.asyncFinish();
                return;
            }
            span.tag(Tags.ofKey("mq.message.id"), sendReceipt.getMessageId().toString());
            span.asyncFinish();
        });
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst,
                                      Method method,
                                      Object[] allArguments,
                                      Class<?>[] argumentsTypes,
                                      Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private String buildOperationName(String topicName) {
        return ASYNC_SEND_OPERATION_NAME_PREFIX + topicName + "/Producer";
    }
}
