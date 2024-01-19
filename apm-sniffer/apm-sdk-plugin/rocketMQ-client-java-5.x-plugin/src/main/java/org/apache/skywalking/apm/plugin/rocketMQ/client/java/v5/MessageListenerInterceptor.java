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

import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
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
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.rocketMQ.client.java.v5.define.ConsumerEnhanceInfos;

import java.lang.reflect.Method;

public class MessageListenerInterceptor implements InstanceMethodsAroundInterceptor {

    public static final String CONSUMER_OPERATION_NAME_PREFIX = "RocketMQ/";
    public static final Gson GSON = new Gson();

    @Override
    public void beforeMethod(EnhancedInstance objInst,
                             Method method,
                             Object[] allArguments,
                             Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        MessageView messageView = (MessageView) allArguments[0];

        ContextCarrier contextCarrier = getContextCarrierFromMessage(messageView);

        AbstractSpan span = ContextManager.createEntrySpan(CONSUMER_OPERATION_NAME_PREFIX + messageView.getTopic()
                                                               + "/Consumer", contextCarrier);
        Tags.MQ_TOPIC.set(span, messageView.getTopic());
        span.tag(Tags.ofKey("mq.message.id"), messageView.getMessageId().toString());
        Object skyWalkingDynamicField = objInst.getSkyWalkingDynamicField();
        if (skyWalkingDynamicField != null) {
            ConsumerEnhanceInfos consumerEnhanceInfos = (ConsumerEnhanceInfos) skyWalkingDynamicField;
            Tags.MQ_BROKER.set(span, consumerEnhanceInfos.getNamesrvAddr());
            span.setPeer(consumerEnhanceInfos.getNamesrvAddr());
        }
        span.setComponent(ComponentsDefine.ROCKET_MQ_CONSUMER);
        SpanLayer.asMQ(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst,
                              Method method,
                              Object[] allArguments,
                              Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ConsumeResult status = (ConsumeResult) ret;
        if (ConsumeResult.FAILURE.equals(status)) {
            AbstractSpan activeSpan = ContextManager.activeSpan();
            activeSpan.errorOccurred();
            Tags.MQ_STATUS.set(activeSpan, status.name());
        }
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst,
                                      Method method,
                                      Object[] allArguments,
                                      Class<?>[] argumentsTypes,
                                      Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private ContextCarrier getContextCarrierFromMessage(MessageView message) {
        ContextCarrier contextCarrier = new ContextCarrier();

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(message.getProperties().get(next.getHeadKey()));
        }

        return contextCarrier;
    }
}
