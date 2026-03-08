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

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
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
import org.mockito.Mock;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

@RunWith(TracingSegmentRunner.class)
public class RabbitMQSpringConsumerInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    private SpringRabbitMQConsumerInterceptor rabbitMQConsumerInterceptor;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    @Before
    public void setUp() throws Exception {
        rabbitMQConsumerInterceptor = new SpringRabbitMQConsumerInterceptor();
    }

    @Test
    public void testRabbitMQConsumerInterceptorWithNilHeaders() throws Throwable {
        Object[] args = prepareMockData(false);
        rabbitMQConsumerInterceptor.beforeMethod(enhancedInstance, null, args, new Class[0], null);
        rabbitMQConsumerInterceptor.afterMethod(enhancedInstance, null, args, new Class[0], null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testRabbitMQConsumerInterceptor() throws Throwable {
        Object[] args = prepareMockData(true);
        rabbitMQConsumerInterceptor.beforeMethod(enhancedInstance, null, args, new Class[0], null);
        rabbitMQConsumerInterceptor.afterMethod(enhancedInstance, null, args, new Class[0], null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    private Object[] prepareMockData(boolean withHeaders) throws Exception {
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        InetAddress address = mock(InetAddress.class);
        Message message = mock(Message.class);
        MessageProperties messageProperties = mock(MessageProperties.class);

        when(channel.getConnection()).thenReturn(connection);
        when(connection.getAddress()).thenReturn(address);
        when(address.getHostAddress()).thenReturn("127.0.0.1");
        when(connection.getPort()).thenReturn(5672);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedExchange()).thenReturn("test-exchange");
        when(messageProperties.getReceivedRoutingKey()).thenReturn("test-routing-key");

        if (withHeaders) {
            Map<String, Object> headers = new HashMap<>();
            headers.put(SW8CarrierItem.HEADER_NAME,
                "1-My40LjU=-MS4yLjM=-3-c2VydmljZQ==-aW5zdGFuY2U=-L2FwcA==-MTI3LjAuMC4xOjgwODA=");
            when(messageProperties.getHeader(SW8CarrierItem.HEADER_NAME))
                .thenReturn(headers.get(SW8CarrierItem.HEADER_NAME));
        }

        return new Object[] {channel, message};
    }
}
