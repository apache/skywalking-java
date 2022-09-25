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

package org.apache.skywalking.apm.plugin.baidu.brpc3;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import java.util.Map;

import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.SW8CarrierItem;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SegmentRefHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class ServerInterceptorTest {
    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    @Mock
    private EnhancedInstance enhancedInstance;

    private ServerInterceptor serverInterceptor;

    private Request request = PowerMockito.mock(Request.class);
    private Response response = PowerMockito.mock(Response.class);

    private InterceptorChain interceptorChain = PowerMockito.mock(InterceptorChain.class);

    @Mock
    private MethodInterceptResult methodInterceptResult;

    private Object[] allArguments;
    private Class[] argumentTypes;

    @Before
    public void setUp() throws Exception {
        serverInterceptor = new ServerInterceptor();

        Map<String, Object> kvAttachment = PowerMockito.mock(Map.class);
        when(kvAttachment.get(SW8CarrierItem.HEADER_NAME)).thenReturn("1-My40LjU=-MS4yLjM=-3-c2VydmljZQ"
                + "==-aW5zdGFuY2U=-L2FwcA==-MTI3LjAuMC4xOjgwODA=");
        when(request.getKvAttachment()).thenReturn(kvAttachment);

        when(request.getMethodName()).thenReturn("testMethod");
        when(request.getServiceName()).thenReturn("testService");
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn("127.0.0.1:8123");

        allArguments = new Object[] {request, response, interceptorChain};
        argumentTypes = new Class[] {request.getClass(), response.getClass(), interceptorChain.getClass()};
        Config.Agent.SERVICE_NAME = "BRPC3-TestCases-APP";
    }

    @Test
    public void testProviderWithAttachment() throws Throwable {
        serverInterceptor.beforeMethod(enhancedInstance, null, allArguments, argumentTypes, methodInterceptResult);
        serverInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentTypes, response);
        assertProvider();
    }

    private void assertProvider() {
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        assertThat(SegmentHelper.getSpans(traceSegment).size(), is(1));
        assertProviderSpan(SegmentHelper.getSpans(traceSegment).get(0));
        assertTraceSegmentRef(traceSegment.getRef());
    }

    private void assertProviderSpan(AbstractTracingSpan span) {
        assertCommonsAttribute(span);
        assertTrue(span.isEntry());
    }

    private void assertCommonsAttribute(AbstractTracingSpan span) {
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.size(), is(0));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.RPC_FRAMEWORK));
        assertThat(SpanHelper.getComponentId(span), is(91));
    }

    private void assertTraceSegmentRef(TraceSegmentRef actual) {
        assertThat(SegmentRefHelper.getSpanId(actual), is(3));
        assertThat(SegmentRefHelper.getParentServiceInstance(actual), is("instance"));
        assertThat(SegmentRefHelper.getTraceSegmentId(actual), is("3.4.5"));
    }

}
