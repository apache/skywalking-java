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

package org.apache.skywalking.apm.plugin.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.skywalking.apm.agent.core.context.SW8CarrierItem;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.hamcrest.CoreMatchers.is;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class RabbitMQConsumerInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    private RabbitMQConsumerInterceptor rabbitMQConsumerInterceptor;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    @Before
    public void setUp() throws Exception {
        rabbitMQConsumerInterceptor = new RabbitMQConsumerInterceptor();
    }

    @Test
    public void testRabbitMQConsumerInterceptorWithNilHeaders() throws Throwable {
        final Object[] args = {
            null,
            null,
            null,
            null,
            null,
            null,
            getConsumer()
        };
        rabbitMQConsumerInterceptor.beforeMethod(getEnhancedInstance(), null, args, new Class[0], null);
        rabbitMQConsumerInterceptor.afterMethod(getEnhancedInstance(), null, args, new Class[0], null);
        ((Consumer) args[6]).handleDelivery("tag", new Envelope(1L, false, "exchange", "routerKey"),
                                            new AMQP.BasicProperties(), new byte[0]
        );
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testRabbitMQConsumerInterceptor() throws Throwable {
        final Object[] args = {
            null,
            null,
            null,
            null,
            null,
            null,
            getConsumer()
        };

        Map<String, Object> headers = new HashMap<>();
        headers.put(
            SW8CarrierItem.HEADER_NAME,
            "1-My40LjU=-MS4yLjM=-3-c2VydmljZQ==-aW5zdGFuY2U=-L2FwcA==-MTI3LjAuMC4xOjgwODA="
        );
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
        propsBuilder.headers(headers);

        rabbitMQConsumerInterceptor.beforeMethod(getEnhancedInstance(), null, args, new Class[0], null);
        rabbitMQConsumerInterceptor.afterMethod(getEnhancedInstance(), null, args, new Class[0], null);
        ((Consumer) args[6]).handleDelivery("tag", new Envelope(1L, false, "exchange", "routerKey"),
                                            propsBuilder.build(), new byte[0]
        );
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    public EnhancedInstance getEnhancedInstance() {
        return new EnhancedInstance() {
            @Override
            public Object getSkyWalkingDynamicField() {
                return "serverAddr";
            }

            @Override
            public void setSkyWalkingDynamicField(final Object value) {

            }
        };
    }

    public Consumer getConsumer() {
        return new Consumer() {
            @Override
            public void handleConsumeOk(final String consumerTag) {

            }

            @Override
            public void handleCancelOk(final String consumerTag) {

            }

            @Override
            public void handleCancel(final String consumerTag) throws IOException {

            }

            @Override
            public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig) {

            }

            @Override
            public void handleRecoverOk(final String consumerTag) {

            }

            @Override
            public void handleDelivery(final String consumerTag,
                                       final Envelope envelope,
                                       final AMQP.BasicProperties properties,
                                       final byte[] body) throws IOException {

            }
        };
    }
}
