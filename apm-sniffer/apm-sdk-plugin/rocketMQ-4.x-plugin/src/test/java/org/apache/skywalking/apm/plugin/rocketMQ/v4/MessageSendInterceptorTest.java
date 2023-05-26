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

package org.apache.skywalking.apm.plugin.rocketMQ.v4;

import static org.apache.rocketmq.common.message.MessageDecoder.NAME_VALUE_SEPARATOR;
import static org.apache.rocketmq.common.message.MessageDecoder.PROPERTY_SEPARATOR;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.client.impl.CommunicationMode;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageDecoder;
import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeader;
import org.apache.skywalking.apm.agent.core.context.SW8ExtensionCarrierItem;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class MessageSendInterceptorTest {

    private MessageSendInterceptor messageSendInterceptor;

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private Object[] arguments;

    private Object[] argumentsWithoutCallback;

    @Mock
    private Message message;

    @Mock
    private SendMessageRequestHeader messageRequestHeader;

    @Mock
    private EnhancedInstance callBack;

    private EnhancedInstance enhancedInstance;

    @Before
    public void setUp() {
        messageSendInterceptor = new MessageSendInterceptor();
        enhancedInstance = new EnhancedInstance() {
            @Override
            public Object getSkyWalkingDynamicField() {
                return "127.0.0.1:6543";
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {

            }
        };

        arguments = new Object[] {
            "127.0.0.1",
            "test",
            message,
            messageRequestHeader,
            null,
            CommunicationMode.ASYNC,
            callBack
        };
        argumentsWithoutCallback = new Object[] {
            "127.0.0.1",
            "test",
            message,
            messageRequestHeader,
            null,
            CommunicationMode.ASYNC,
            null
        };
        when(message.getTags()).thenReturn("TagA");
        stubMessageRequestHeader("TAGS" + NAME_VALUE_SEPARATOR + "TagA" + PROPERTY_SEPARATOR);
    }

    @Test
    public void testSendMessage() throws Throwable {
        messageSendInterceptor.beforeMethod(enhancedInstance, null, arguments, null, null);
        messageSendInterceptor.afterMethod(enhancedInstance, null, arguments, null, null);

        Map<String, String> tags = MessageDecoder.string2messageProperties(
            ((SendMessageRequestHeader) arguments[3]).getProperties());
        // check original header of TAGS
        assertThat(tags.get("TAGS"), is("TagA"));
        // check skywalking header
        assertTrue(tags.containsKey(SW8ExtensionCarrierItem.HEADER_NAME));

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        AbstractTracingSpan mqSpan = spans.get(0);

        SpanAssert.assertLayer(mqSpan, SpanLayer.MQ);
        SpanAssert.assertComponent(mqSpan, ComponentsDefine.ROCKET_MQ_PRODUCER);
        SpanAssert.assertTag(mqSpan, 0, "127.0.0.1");
        verify(callBack).setSkyWalkingDynamicField(any());
    }

    @Test
    public void testSendMessageNew() throws Throwable {
        stubMessageRequestHeader("TAGS" + NAME_VALUE_SEPARATOR + "TagA");
        testSendMessage();
    }

    @Test
    public void testSendMessageWithoutCallBack() throws Throwable {
        messageSendInterceptor.beforeMethod(enhancedInstance, null, argumentsWithoutCallback, null, null);
        messageSendInterceptor.afterMethod(enhancedInstance, null, argumentsWithoutCallback, null, null);

        Map<String, String> tags = MessageDecoder.string2messageProperties(
            ((SendMessageRequestHeader) argumentsWithoutCallback[3]).getProperties());
        // check original header of TAGS
        assertThat(tags.get("TAGS"), is("TagA"));
        // check skywalking header
        assertTrue(tags.containsKey(SW8ExtensionCarrierItem.HEADER_NAME));

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        AbstractTracingSpan mqSpan = spans.get(0);

        SpanAssert.assertLayer(mqSpan, SpanLayer.MQ);
        SpanAssert.assertComponent(mqSpan, ComponentsDefine.ROCKET_MQ_PRODUCER);
        SpanAssert.assertTag(mqSpan, 0, "127.0.0.1");
    }

    @Test
    public void testSendMessageWithoutCallBackNew() throws Throwable {
        stubMessageRequestHeader("TAGS" + NAME_VALUE_SEPARATOR + "TagA");
        testSendMessageWithoutCallBack();
    }

    private void stubMessageRequestHeader(String properties) {
        messageRequestHeader = mock(SendMessageRequestHeader.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            String val = (String) invocation.getArguments()[0];
            when(messageRequestHeader.getProperties()).thenReturn(val);
            return null;
        }).when(messageRequestHeader).setProperties(anyString());
        when(messageRequestHeader.getProperties()).thenCallRealMethod();
        messageRequestHeader.setProperties(properties);

        arguments[3] = messageRequestHeader;
        argumentsWithoutCallback[3] = messageRequestHeader;
    }
}
