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

package org.apache.skywalking.apm.plugin.sofarpc;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.ConsumerInvoker;
import java.util.List;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class SofaRpcConsumerInterceptorTest {

    private static MockedStatic<RpcInternalContext> MOCKED_STATIC;

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EnhancedInstance enhancedInstance;

    private SofaRpcConsumerInterceptor sofaRpcConsumerInterceptor;

    @Mock
    private RpcInternalContext rpcContext;

    @Mock
    private ConsumerInvoker invoker;

    @Mock
    private SofaRequest sofaRequest;
    @Mock
    private MethodInterceptResult methodInterceptResult;
    @Mock
    private SofaResponse sofaResponse;

    private Object[] allArguments;
    private Class[] argumentTypes;

    @BeforeClass
    public static void beforeClass() {
        MOCKED_STATIC = Mockito.mockStatic(RpcInternalContext.class);
    }

    @AfterClass
    public static void afterClass() {
        MOCKED_STATIC.close();
    }

    @Before
    public void setUp() throws Exception {
        sofaRpcConsumerInterceptor = new SofaRpcConsumerInterceptor();

        when(sofaRequest.getMethodName()).thenReturn("test");
        when(sofaRequest.getMethodArgSigs()).thenReturn(new String[] {"String"});
        when(sofaRequest.getMethodArgs()).thenReturn(new Object[] {"abc"});
        when(sofaRequest.getInterfaceName()).thenReturn("org.apache.skywalking.apm.test.TestSofaRpcService");
        MOCKED_STATIC.when(RpcInternalContext::getContext).thenReturn(rpcContext);
        when(rpcContext.isConsumerSide()).thenReturn(true);
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setHost("127.0.0.1");
        providerInfo.setPort(12200);
        when(rpcContext.getProviderInfo()).thenReturn(providerInfo);
        allArguments = new Object[] {sofaRequest};
        argumentTypes = new Class[] {sofaRequest.getClass()};
        Config.Agent.SERVICE_NAME = "SOFARPC-TestCases-APP";
    }

    @Test
    public void testConsumerWithAttachment() throws Throwable {
        sofaRpcConsumerInterceptor.beforeMethod(
            enhancedInstance, null, allArguments, argumentTypes, methodInterceptResult);
        sofaRpcConsumerInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentTypes, sofaResponse);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertConsumerSpan(spans.get(0));
    }

    @Test
    public void testConsumerWithException() throws Throwable {
        sofaRpcConsumerInterceptor.beforeMethod(
            enhancedInstance, null, allArguments, argumentTypes, methodInterceptResult);
        sofaRpcConsumerInterceptor.handleMethodException(
            enhancedInstance, null, allArguments, argumentTypes, new RuntimeException());
        sofaRpcConsumerInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentTypes, sofaResponse);
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        assertConsumerTraceSegmentInErrorCase(traceSegment);
    }

    @Test
    public void testConsumerWithResultHasException() throws Throwable {
        when(sofaResponse.isError()).thenReturn(true);
        when(sofaResponse.getAppResponse()).thenReturn(new RuntimeException());

        sofaRpcConsumerInterceptor.beforeMethod(
            enhancedInstance, null, allArguments, argumentTypes, methodInterceptResult);
        sofaRpcConsumerInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentTypes, sofaResponse);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        assertConsumerTraceSegmentInErrorCase(traceSegment);
    }

    private void assertConsumerTraceSegmentInErrorCase(TraceSegment traceSegment) {
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertConsumerSpan(spans.get(0));
        AbstractTracingSpan span = spans.get(0);
        assertThat(SpanHelper.getLogs(span).size(), is(1));
        assertErrorLog(SpanHelper.getLogs(span).get(0));
    }

    private void assertErrorLog(LogDataEntity logData) {
        assertThat(logData.getLogs().size(), is(4));
        assertThat(logData.getLogs().get(0).getValue(), CoreMatchers.<Object>is("error"));
        assertThat(logData.getLogs().get(1).getValue(), CoreMatchers.<Object>is(RuntimeException.class.getName()));
        assertNull(logData.getLogs().get(2).getValue());
    }

    private void assertConsumerSpan(AbstractTracingSpan span) {
        assertCommonsAttribute(span);
        assertTrue(span.isExit());
    }

    private void assertCommonsAttribute(AbstractTracingSpan span) {
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.size(), is(1));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.RPC_FRAMEWORK));
        assertThat(SpanHelper.getComponentId(span), is(43));
        assertThat(
            tags.get(0)
                .getValue(),
            is("bolt://127.0.0.1:12200/org.apache.skywalking.apm.test.TestSofaRpcService.test(String)")
        );
        assertThat(span.getOperationName(), is("org.apache.skywalking.apm.test.TestSofaRpcService.test(String)"));
    }
}
