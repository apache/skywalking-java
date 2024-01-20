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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.rocketMQ.client.java.v5.define.ConsumerEnhanceInfos;

/**
 * {@link SimpleConsumerImplAsyncInterceptor} create local span when the method {@link
 * org.apache.rocketmq.client.java.impl.consumer.SimpleConsumerImpl#receiveAsync(int, Duration)} execute.
 */
public class SimpleConsumerImplAsyncInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {
    public static final String CONSUMER_OPERATION_NAME_PREFIX = "RocketMQ/";

    @Override
    public final void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                                   Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        CompletableFuture<List<MessageView>> futureList = (CompletableFuture<List<MessageView>>) ret;
        ContextSnapshot capture = null;
        if (ContextManager.isActive()) {
            capture = ContextManager.capture();
        }
        final ContextSnapshot finalCapture = capture;
        return futureList.whenCompleteAsync((messages, throwable) -> {
            String topics = messages.stream().map(MessageView::getTopic).distinct().collect(Collectors.joining(","));
            AbstractSpan span = ContextManager.createEntrySpan(
                CONSUMER_OPERATION_NAME_PREFIX + topics + "/Consumer", null);
            if (finalCapture != null) {
                ContextManager.continued(finalCapture);
            }
            if (null != throwable) {
                span.log(throwable);
                span.errorOccurred();
                ContextManager.stopSpan();
                return;
            }
            if (messages.isEmpty()) {
                ContextManager.stopSpan();
                return;
            }
            String namesrvAddr = "";
            Object skyWalkingDynamicField = objInst.getSkyWalkingDynamicField();
            if (skyWalkingDynamicField != null) {
                ConsumerEnhanceInfos consumerEnhanceInfos = (ConsumerEnhanceInfos) objInst.getSkyWalkingDynamicField();
                namesrvAddr = consumerEnhanceInfos.getNamesrvAddr();
            }
            SpanLayer.asMQ(span);
            Tags.MQ_BROKER.set(span, namesrvAddr);
            Tags.MQ_TOPIC.set(span, topics);
            span.setPeer(namesrvAddr);
            span.setComponent(ComponentsDefine.ROCKET_MQ_CONSUMER);

            for (MessageView messageView : messages) {
                ContextCarrier contextCarrier = getContextCarrierFromMessage(messageView);
                ContextManager.extract(contextCarrier);
            }
            ContextManager.stopSpan();
        });
    }

    @Override
    public final void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                            Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    @Override
    public void onConstruct(final EnhancedInstance objInst, final Object[] allArguments) throws Throwable {
        ClientConfiguration clientConfiguration = (ClientConfiguration) allArguments[0];
        String namesrvAddr = clientConfiguration.getEndpoints();
        ConsumerEnhanceInfos consumerEnhanceInfos = new ConsumerEnhanceInfos(namesrvAddr);
        objInst.setSkyWalkingDynamicField(consumerEnhanceInfos);
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
