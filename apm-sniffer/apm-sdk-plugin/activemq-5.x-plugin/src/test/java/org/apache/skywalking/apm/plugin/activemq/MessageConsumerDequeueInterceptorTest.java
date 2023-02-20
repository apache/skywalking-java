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

package org.apache.skywalking.apm.plugin.activemq;

import java.util.List;
import javax.jms.JMSException;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.Response;
import org.apache.activemq.state.CommandVisitor;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;

@RunWith(TracingSegmentRunner.class)
public class MessageConsumerDequeueInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    public class Des extends ActiveMQDestination {

        @Override
        protected String getQualifiedPrefix() {
            return null;
        }

        @Override
        public byte getDestinationType() {
            return 1;
        }

        @Override
        public byte getDataStructureType() {
            return 0;
        }
    }

    public class Msg extends Message {

        @Override
        public Message copy() {
            return null;
        }

        @Override
        public void clearBody() throws JMSException {

        }

        @Override
        public void storeContent() {

        }

        @Override
        public void storeContentAndClear() {

        }

        @Override
        public Response visit(CommandVisitor commandVisitor) throws Exception {
            return null;
        }

        @Override
        public byte getDataStructureType() {
            return 0;
        }
    }

    private EnhancedInstance enhancedInstance = new EnhancedInstance() {
        @Override
        public Object getSkyWalkingDynamicField() {
            return "localhost:60601";
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
        }
    };

    @Test
    public void testConsumerWithoutMessage() throws Throwable {
        MessageDispatch  messageDispatch = new MessageDispatch();
        Des des = new Des();
        des.setPhysicalName("test");
        messageDispatch.setDestination(des);
        messageDispatch.setMessage(new Msg());
        new MessageConsumerDequeueInterceptor().afterMethod(enhancedInstance, null, new Object[0], null, messageDispatch);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

}
