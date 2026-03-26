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

package org.apache.skywalking.apm.plugin.httpClient.v4;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.httpclient.HttpClientPluginConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Verifies that requests to ports listed in
 * {@link HttpClientPluginConfig.Plugin.HttpClient#PROPAGATION_EXCLUDE_PORTS}
 * are silently skipped (no span created, no sw8 header injected).
 */
@RunWith(TracingSegmentRunner.class)
public class HttpClientPropagationExcludePortTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private HttpHost clickHouseHost;
    @Mock
    private HttpHost regularHost;
    @Mock
    private HttpGet request;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private StatusLine statusLine;
    @Mock
    private EnhancedInstance enhancedInstance;

    private HttpClientExecuteInterceptor interceptor;

    private Object[] clickHouseArgs;
    private Object[] regularArgs;
    private Class<?>[] argumentsType;

    @Before
    public void setUp() throws Exception {
        ServiceManager.INSTANCE.boot();

        HttpClientPluginConfig.Plugin.HttpClient.PROPAGATION_EXCLUDE_PORTS = "8123";
        interceptor = new HttpClientExecuteInterceptor();

        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        RequestLine requestLine = new RequestLine() {
            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public String getUri() {
                return "http://my-service:8080/api/ping";
            }
        };
        when(request.getRequestLine()).thenReturn(requestLine);

        when(clickHouseHost.getHostName()).thenReturn("clickhouse-server");
        when(clickHouseHost.getSchemeName()).thenReturn("http");
        when(clickHouseHost.getPort()).thenReturn(8123);

        when(regularHost.getHostName()).thenReturn("my-service");
        when(regularHost.getSchemeName()).thenReturn("http");
        when(regularHost.getPort()).thenReturn(8080);

        clickHouseArgs = new Object[]{clickHouseHost, request};
        regularArgs = new Object[]{regularHost, request};
        argumentsType = new Class[]{HttpHost.class, HttpGet.class};
    }

    @Test
    public void requestToExcludedPort_noSpanAndNoHeaderInjected() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        interceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat(segments.size(), is(0));
        verify(request, never()).setHeader(anyString(), anyString());
    }

    @Test
    public void requestToRegularPort_spanCreatedAndHeadersInjected() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, null, regularArgs, argumentsType, null);
        interceptor.afterMethod(enhancedInstance, null, regularArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat(segments.size(), is(1));
        verify(request, times(3)).setHeader(anyString(), anyString());
    }

    @Test
    public void whenExcludePortsEmpty_allPortsAreTraced() throws Throwable {
        HttpClientPluginConfig.Plugin.HttpClient.PROPAGATION_EXCLUDE_PORTS = "";

        HttpClientExecuteInterceptor freshInterceptor = new HttpClientExecuteInterceptor();
        freshInterceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat(segments.size(), is(1));
    }

    @Test
    public void multipleExcludedPorts_allSkippedAndNonExcludedStillTraced() throws Throwable {
        HttpClientPluginConfig.Plugin.HttpClient.PROPAGATION_EXCLUDE_PORTS = "8123,9200";

        HttpClientExecuteInterceptor freshInterceptor = new HttpClientExecuteInterceptor();

        // 8123 should be excluded
        freshInterceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);
        assertThat(segmentStorage.getTraceSegments().size(), is(0));

        // 9200 should also be excluded
        HttpHost esHost = mock(HttpHost.class);
        when(esHost.getHostName()).thenReturn("es-server");
        when(esHost.getSchemeName()).thenReturn("http");
        when(esHost.getPort()).thenReturn(9200);
        Object[] esArgs = new Object[]{esHost, request};

        freshInterceptor = new HttpClientExecuteInterceptor();
        HttpClientPluginConfig.Plugin.HttpClient.PROPAGATION_EXCLUDE_PORTS = "8123,9200";
        freshInterceptor.beforeMethod(enhancedInstance, null, esArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, esArgs, argumentsType, httpResponse);
        assertThat(segmentStorage.getTraceSegments().size(), is(0));

        // 8080 should still be traced
        freshInterceptor = new HttpClientExecuteInterceptor();
        HttpClientPluginConfig.Plugin.HttpClient.PROPAGATION_EXCLUDE_PORTS = "8123,9200";
        freshInterceptor.beforeMethod(enhancedInstance, null, regularArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, regularArgs, argumentsType, httpResponse);
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
    }

    @Test
    public void excludedPort_handleMethodExceptionSkipped() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        interceptor.handleMethodException(enhancedInstance, null, clickHouseArgs, argumentsType, new RuntimeException("test"));
        interceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat(segments.size(), is(0));
    }
}
