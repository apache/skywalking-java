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

package org.apache.skywalking.apm.plugin.guava.eventbus;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.apache.skywalking.apm.agent.core.context.MockContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class EventBusSubscriberInterceptorTest {

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    private EventBusSubscriberInterceptor interceptor;
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    private EventWrapper eventWrapper;
    private EnhancedInstance enhancedInstance;
    private SubscriberInfo subscriberInfo;

    @Before
    public void setUp() throws Exception {
        interceptor = new EventBusSubscriberInterceptor();
        eventWrapper = EventWrapper.wrapEvent(new Object(), MockContextSnapshot.INSTANCE.mockContextSnapshot());
        enhancedInstance = new EnhancedInstance() {
            private Object value;

            @Override
            public Object getSkyWalkingDynamicField() {
                return value;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.value = value;
            }
        };
        subscriberInfo = new SubscriberInfo();
        subscriberInfo.setMethodName("testMethodName");
        subscriberInfo.setClassName("testClassName");
    }

    @Test
    public void test() throws Throwable {
        Object[] arguments = new Object[] {eventWrapper};
        enhancedInstance.setSkyWalkingDynamicField(subscriberInfo);
        interceptor.beforeMethod(enhancedInstance, null, arguments, new Class[1], null);
        interceptor.afterMethod(null, null, null, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        final TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans, notNullValue());
        assertThat(spans.size(), is(1));
    }
}