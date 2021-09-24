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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.net.URI;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
@PrepareForTest(HttpHost.class)
public class HttpClientExecuteInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    private HttpClientDoExecuteInterceptor httpClientDoExecuteInterceptor;

    @Mock
    private HttpHost httpHost;
    @Mock
    private ClassicHttpRequest request;
    @Mock
    private ClassicHttpResponse httpResponse;

    private Object[] allArguments;
    private Class[] argumentsType;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Before
    public void setUp() throws Exception {

        ServiceManager.INSTANCE.boot();
        httpClientDoExecuteInterceptor = new HttpClientDoExecuteInterceptor();

        PowerMockito.mock(HttpHost.class);
        when(httpResponse.getCode()).thenReturn(200);
        when(httpHost.getHostName()).thenReturn("127.0.0.1");
        when(httpHost.getSchemeName()).thenReturn("http");
        when(request.getUri()).thenReturn(new URI("http://127.0.0.1:8080/test-web/test"));
        when(request.getMethod()).thenReturn("GET");
        when(httpHost.getPort()).thenReturn(8080);

        allArguments = new Object[]{
                httpHost,
                request
        };
        argumentsType = new Class[]{
                httpHost.getClass(),
                request.getClass()
        };
    }

    @Test
    public void testHttpClient() throws Throwable {
        httpClientDoExecuteInterceptor.beforeMethod(enhancedInstance, null, allArguments, argumentsType, null);
        httpClientDoExecuteInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentsType, httpResponse);

        Assert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertHttpSpan(spans.get(0));
        verify(request, times(3)).setHeader(anyString(), anyString());
    }

    @Test
    public void testStatusCodeNotEquals200() throws Throwable {
        when(httpResponse.getCode()).thenReturn(500);
        httpClientDoExecuteInterceptor.beforeMethod(enhancedInstance, null, allArguments, argumentsType, null);
        httpClientDoExecuteInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentsType, httpResponse);

        Assert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertThat(spans.size(), is(1));

        List<TagValuePair> tags = SpanHelper.getTags(spans.get(0));
        assertThat(tags.size(), is(3));
        assertThat(tags.get(2).getValue(), is("500"));

        assertHttpSpan(spans.get(0));
        assertThat(SpanHelper.getErrorOccurred(spans.get(0)), is(true));
        verify(request, times(3)).setHeader(anyString(), anyString());
    }

    @Test
    public void testHttpClientWithException() throws Throwable {
        httpClientDoExecuteInterceptor.beforeMethod(enhancedInstance, null, allArguments, argumentsType, null);
        httpClientDoExecuteInterceptor.handleMethodException(enhancedInstance, null, allArguments, argumentsType,
                new RuntimeException("testException"));
        httpClientDoExecuteInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentsType, httpResponse);

        Assert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertThat(spans.size(), is(1));
        AbstractTracingSpan span = spans.get(0);
        assertHttpSpan(span);
        assertThat(SpanHelper.getErrorOccurred(span), is(true));
        assertHttpSpanErrorLog(SpanHelper.getLogs(span));
        verify(request, times(3)).setHeader(anyString(), anyString());

    }

    @Test
    public void testUriNotProtocol() throws Throwable {
        when(request.getUri()).thenReturn(new URI("/test-web/test"));
        httpClientDoExecuteInterceptor.beforeMethod(enhancedInstance, null, allArguments, argumentsType, null);
        httpClientDoExecuteInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentsType, httpResponse);

        Assert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertHttpSpan(spans.get(0));
        verify(request, times(3)).setHeader(anyString(), anyString());
    }

    private void assertHttpSpanErrorLog(List<LogDataEntity> logs) {
        assertThat(logs.size(), is(1));
        LogDataEntity logData = logs.get(0);
        Assert.assertThat(logData.getLogs().size(), is(4));
        Assert.assertThat(logData.getLogs().get(0).getValue(), CoreMatchers.<Object>is("error"));
        Assert.assertThat(logData.getLogs()
                .get(1)
                .getValue(), CoreMatchers.<Object>is(RuntimeException.class.getName()));
        Assert.assertThat(logData.getLogs().get(2).getValue(), is("testException"));
        assertNotNull(logData.getLogs().get(3).getValue());
    }

    private void assertHttpSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), is("/test-web/test"));
        assertThat(SpanHelper.getComponentId(span), is(2));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("http://127.0.0.1:8080/test-web/test"));
        assertThat(tags.get(1).getValue(), is("GET"));
        assertThat(span.isExit(), is(true));
    }

}
