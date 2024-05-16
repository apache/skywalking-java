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

package org.apache.skywalking.apm.plugin.rocketMQ.v3;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.MockContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.rocketMQ.v3.define.SendCallBackEnhanceInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class OnExceptionInterceptorTest {

    private OnExceptionInterceptor exceptionInterceptor;

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ContextSnapshot contextSnapshot;
    private SendCallBackEnhanceInfo enhanceInfo;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Before
    public void setUp() {
        exceptionInterceptor = new OnExceptionInterceptor();
        contextSnapshot = MockContextSnapshot.INSTANCE.mockContextSnapshot();

        enhanceInfo = new SendCallBackEnhanceInfo("test", contextSnapshot);
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn(enhanceInfo);
    }

    @Test
    public void testOnException() throws Throwable {
        exceptionInterceptor.beforeMethod(enhancedInstance, null, new Object[] {new RuntimeException()}, null, null);
        exceptionInterceptor.afterMethod(enhancedInstance, null, new Object[] {new RuntimeException()}, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        AbstractTracingSpan exceptionSpan = spans.get(0);
        SpanAssert.assertException(SpanHelper.getLogs(exceptionSpan).get(0), RuntimeException.class);
        SpanAssert.assertOccurException(exceptionSpan, true);
    }
}
