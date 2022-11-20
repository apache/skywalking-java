/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

public class TracerConsumer implements Consumer {

    private Consumer delegate;
    private String serverUrl;

    public static final String OPERATE_NAME_PREFIX = "RabbitMQ/";
    public static final String CONSUMER_OPERATE_NAME_SUFFIX = "/Consumer";

    public TracerConsumer(final Consumer delegate, final String serverUrl) {
        this.delegate = delegate;
        this.serverUrl = serverUrl;
    }

    @Override
    public void handleConsumeOk(final String consumerTag) {
        this.delegate.handleConsumeOk(consumerTag);
    }

    @Override
    public void handleCancelOk(final String consumerTag) {
        this.delegate.handleRecoverOk(consumerTag);
    }

    @Override
    public void handleCancel(final String consumerTag) throws IOException {
        this.delegate.handleCancel(consumerTag);
    }

    @Override
    public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig) {
        this.delegate.handleShutdownSignal(consumerTag, sig);
    }

    @Override
    public void handleRecoverOk(final String consumerTag) {
        this.delegate.handleRecoverOk(consumerTag);
    }

    @Override
    public void handleDelivery(final String consumerTag,
                               final Envelope envelope,
                               final AMQP.BasicProperties properties,
                               final byte[] body) throws IOException {

        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan activeSpan = ContextManager.createEntrySpan(
            OPERATE_NAME_PREFIX + "Topic/" + envelope.getExchange() + "Queue/" + envelope
                .getRoutingKey() + CONSUMER_OPERATE_NAME_SUFFIX, null).start(System.currentTimeMillis());
        Tags.MQ_BROKER.set(activeSpan, serverUrl);
        Tags.MQ_TOPIC.set(activeSpan, envelope.getExchange());
        Tags.MQ_QUEUE.set(activeSpan, envelope.getRoutingKey());
        activeSpan.setComponent(ComponentsDefine.RABBITMQ_CONSUMER);
        activeSpan.setPeer(serverUrl);
        SpanLayer.asMQ(activeSpan);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (properties.getHeaders() != null && properties.getHeaders().get(next.getHeadKey()) != null) {
                next.setHeadValue(properties.getHeaders().get(next.getHeadKey()).toString());
            }
        }
        ContextManager.extract(contextCarrier);
        try {
            this.delegate.handleDelivery(consumerTag, envelope, properties, body);
        } catch (Exception e) {
            activeSpan.log(e).errorOccurred();
        } finally {
            ContextManager.stopSpan(activeSpan);
        }
    }
}
