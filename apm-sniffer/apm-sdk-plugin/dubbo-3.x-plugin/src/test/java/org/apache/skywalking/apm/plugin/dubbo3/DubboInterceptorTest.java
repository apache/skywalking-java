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

package org.apache.skywalking.apm.plugin.dubbo3;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;

import java.util.List;

import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextManagerExtendService;
import org.apache.skywalking.apm.agent.core.context.SW8CarrierItem;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
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
import org.apache.skywalking.apm.plugin.asf.dubbo3.DubboInterceptor;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.util.Assert;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
@PrepareForTest({RpcContext.class})
public class DubboInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @Mock
    private EnhancedInstance enhancedInstance;

    private DubboInterceptor dubboInterceptor;

    @Mock
    private RpcServiceContext serviceContext;
    @Mock
    private RpcContextAttachment contextAttachment;
    @Mock
    private Invoker consumerInvoker;
    @Mock
    private Invoker providerInvoker;
    @Mock
    private Invocation consumerInvocation;
    @Mock
    private Invocation providerInvocation;
    @Mock
    private MethodInterceptResult methodInterceptResult;
    @Mock
    private Result result;

    private Object[] consumerArguments;
    private Object[] providerArguments;
    private Class[] argumentTypes;
    private static final URL CONSUMER_URL = URL.valueOf("dubbo://127.0.0.1:20880/org.apache.skywalking.apm.test.TestDubboService?side=consumer");
    private static final URL PROVIDER_URL = URL.valueOf("dubbo://127.0.0.1:20880/org.apache.skywalking.apm.test.TestDubboService?side=provider");

    @Before
    public void setUp() throws Exception {
        dubboInterceptor = new DubboInterceptor();

        PowerMockito.mockStatic(RpcContext.class);
        when(consumerInvoker.getUrl()).thenReturn(CONSUMER_URL);
        when(consumerInvocation.getMethodName()).thenReturn("test");
        when(consumerInvocation.getParameterTypes()).thenReturn(new Class[] {String.class});
        when(consumerInvocation.getArguments()).thenReturn(new Object[] {"abc"});
        when(consumerInvocation.getInvoker()).thenReturn(consumerInvoker);
        when(RpcContext.getClientAttachment()).thenReturn(contextAttachment);
        consumerArguments = new Object[] {
                consumerInvoker,
                consumerInvocation
        };

        when(providerInvoker.getUrl()).thenReturn(PROVIDER_URL);
        when(providerInvocation.getMethodName()).thenReturn("test");
        when(providerInvocation.getParameterTypes()).thenReturn(new Class[] {String.class});
        when(providerInvocation.getArguments()).thenReturn(new Object[] {"abc"});
        when(providerInvocation.getInvoker()).thenReturn(providerInvoker);
        providerArguments = new Object[] {
                providerInvoker,
                providerInvocation
        };
        argumentTypes = new Class[] {
            consumerInvoker.getClass(),
            consumerInvocation.getClass()
        };
        Config.Agent.SERVICE_NAME = "DubboTestCases-APP";
    }

    @Test
    public void testServiceFromPlugin() {
        PluginBootService service = ServiceManager.INSTANCE.findService(PluginBootService.class);

        Assert.notNull(service);
    }

    @Test
    public void testServiceOverrideFromPlugin() {
        ContextManagerExtendService service = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);

        Assert.isInstanceOf(ContextManagerExtendOverrideService.class, service);
    }

    @Test
    public void testConsumerWithAttachment() throws Throwable {
        dubboInterceptor.beforeMethod(enhancedInstance, null, consumerArguments, argumentTypes, methodInterceptResult);
        dubboInterceptor.afterMethod(enhancedInstance, null, consumerArguments, argumentTypes, result);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertConsumerSpan(spans.get(0));
    }

    @Test
    public void testConsumerWithException() throws Throwable {
        dubboInterceptor.beforeMethod(enhancedInstance, null, consumerArguments, argumentTypes, methodInterceptResult);
        dubboInterceptor.handleMethodException(
            enhancedInstance, null, consumerArguments, argumentTypes, new RuntimeException());
        dubboInterceptor.afterMethod(enhancedInstance, null, consumerArguments, argumentTypes, result);
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        assertConsumerTraceSegmentInErrorCase(traceSegment);
    }

    @Test
    public void testConsumerWithResultHasException() throws Throwable {
        when(result.getException()).thenReturn(new RuntimeException());

        dubboInterceptor.beforeMethod(enhancedInstance, null, consumerArguments, argumentTypes, methodInterceptResult);
        dubboInterceptor.afterMethod(enhancedInstance, null, consumerArguments, argumentTypes, result);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        assertConsumerTraceSegmentInErrorCase(traceSegment);
    }

    @Test
    public void testProviderWithAttachment() throws Throwable {
        when(providerInvoker.getUrl()).thenReturn(PROVIDER_URL);
        when(RpcContext.getServerAttachment()).thenReturn(contextAttachment);
        when(contextAttachment.getAttachment(
            SW8CarrierItem.HEADER_NAME)).thenReturn(
            "1-My40LjU=-MS4yLjM=-3-c2VydmljZQ==-aW5zdGFuY2U=-L2FwcA==-MTI3LjAuMC4xOjgwODA=");

        dubboInterceptor.beforeMethod(enhancedInstance, null, providerArguments, argumentTypes, methodInterceptResult);
        dubboInterceptor.afterMethod(enhancedInstance, null, providerArguments, argumentTypes, result);
        assertProvider();
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

    private void assertProvider() {
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        assertThat(SegmentHelper.getSpans(traceSegment).size(), is(1));
        assertProviderSpan(SegmentHelper.getSpans(traceSegment).get(0));
        assertTraceSegmentRef(traceSegment.getRef());
    }

    private void assertTraceSegmentRef(TraceSegmentRef actual) {
        assertThat(SegmentRefHelper.getSpanId(actual), is(3));
        assertThat(SegmentRefHelper.getParentServiceInstance(actual), is("instance"));
        assertThat(SegmentRefHelper.getTraceSegmentId(actual), is("3.4.5"));
    }

    private void assertProviderSpan(AbstractTracingSpan span) {
        assertCommonsAttribute(span);
        assertTrue(span.isEntry());
    }

    private void assertConsumerSpan(AbstractTracingSpan span) {
        assertCommonsAttribute(span);
        assertTrue(span.isExit());
    }

    private void assertCommonsAttribute(AbstractTracingSpan span) {
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.size(), is(1));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.RPC_FRAMEWORK));
        assertThat(SpanHelper.getComponentId(span), is(3));
        assertThat(
            tags.get(0)
                .getValue(),
            is("dubbo://127.0.0.1:20880/org.apache.skywalking.apm.test.TestDubboService.test(String)")
        );
        assertThat(span.getOperationName(), is("org.apache.skywalking.apm.test.TestDubboService.test(String)"));
    }
}
