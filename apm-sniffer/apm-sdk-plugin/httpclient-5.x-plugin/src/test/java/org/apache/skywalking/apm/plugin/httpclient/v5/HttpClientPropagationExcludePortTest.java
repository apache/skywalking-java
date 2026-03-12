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
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that requests to ports listed in
 * {@link HttpClient5PluginConfig.Plugin.HttpClient5#PROPAGATION_EXCLUDE_PORTS}
 * are silently skipped (no span created, no {@code sw8} header injected).
 *
 * <p>This regression-covers the ClickHouse HTTP-interface issue: ClickHouse
 * listens on port 8123 and rejects HTTP requests that carry unknown headers
 * (such as the SkyWalking {@code sw8} propagation header), responding with
 * HTTP 400 Bad Request.  By excluding port 8123 the agent leaves those
 * requests untouched while continuing to trace all other HTTP calls.
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
    private HttpHost clickHouseHost;          // port 8123 – should be excluded
    @Mock
    private HttpHost regularHost;             // port 8080 – should be traced
    @Mock
    private ClassicHttpRequest request;
    @Mock
    private ClassicHttpResponse httpResponse;
    @Mock
    private EnhancedInstance enhancedInstance;

    private HttpClientDoExecuteInterceptor internalInterceptor;
    private HttpClientDoExecuteInterceptor minimalInterceptor;

    private Object[] clickHouseArgs;
    private Object[] regularArgs;
    private Class<?>[] argumentsType;

    @Before
    public void setUp() throws Exception {
        ServiceManager.INSTANCE.boot();

        // Set the exclusion list to the default (includes 8123)
        HttpClient5PluginConfig.Plugin.HttpClient5.PROPAGATION_EXCLUDE_PORTS = "8123";

        internalInterceptor = new InternalClientDoExecuteInterceptor();
        minimalInterceptor  = new MinimalClientDoExecuteInterceptor();

        when(httpResponse.getCode()).thenReturn(200);

        // ClickHouse-like host on port 8123
        when(clickHouseHost.getHostName()).thenReturn("clickhouse-server");
        when(clickHouseHost.getSchemeName()).thenReturn("http");
        when(clickHouseHost.getPort()).thenReturn(8123);

        // Regular application host on port 8080
        when(regularHost.getHostName()).thenReturn("my-service");
        when(regularHost.getSchemeName()).thenReturn("http");
        when(regularHost.getPort()).thenReturn(8080);

        when(request.getUri()).thenReturn(new URI("http://my-service:8080/api/ping"));
        when(request.getMethod()).thenReturn("GET");

        clickHouseArgs  = new Object[]{clickHouseHost, request};
        regularArgs     = new Object[]{regularHost,    request};
        argumentsType   = new Class[]{HttpHost.class, ClassicHttpRequest.class};
    }

    // -----------------------------------------------------------------------
    // InternalHttpClient path
    // -----------------------------------------------------------------------

    /**
     * Requests to port 8123 via {@code InternalHttpClient} must not produce a
     * trace segment and must NOT set any propagation header on the request.
     *
     * <p>Before this fix the agent injected {@code sw8} (and two companion
     * headers) into every outbound request regardless of the destination port.
     * ClickHouse interprets unknown headers as malformed requests and returns
     * HTTP 400, making all JDBC queries fail while the SkyWalking agent is
     * attached.
     */
    @Test
    public void internalClient_requestToExcludedPort_noSpanAndNoHeaderInjected() throws Throwable {
        internalInterceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        internalInterceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat("No trace segment should be created for excluded port", segments.size(), is(0));
        verify(request, never()).setHeader(anyString(), anyString());
    }

    /**
     * Requests to a non-excluded port via {@code InternalHttpClient} must still
     * be traced and have propagation headers injected.
     */
    @Test
    public void internalClient_requestToRegularPort_spanCreatedAndHeadersInjected() throws Throwable {
        internalInterceptor.beforeMethod(enhancedInstance, null, regularArgs, argumentsType, null);
        internalInterceptor.afterMethod(enhancedInstance, null, regularArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat("A trace segment must be created for a non-excluded port", segments.size(), is(1));
        // sw8, sw8-correlation, sw8-x – exactly 3 propagation headers, consistent with existing tests
        verify(request, org.mockito.Mockito.times(3)).setHeader(anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // MinimalHttpClient path
    // -----------------------------------------------------------------------

    /**
     * Same assertion for the {@code MinimalHttpClient} code path.
     */
    @Test
    public void minimalClient_requestToExcludedPort_noSpanAndNoHeaderInjected() throws Throwable {
        minimalInterceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        minimalInterceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat("No trace segment should be created for excluded port", segments.size(), is(0));
        verify(request, never()).setHeader(anyString(), anyString());
    }

    /**
     * Normal (non-excluded) port via {@code MinimalHttpClient} must still be
     * traced.
     */
    @Test
    public void minimalClient_requestToRegularPort_spanCreatedAndHeadersInjected() throws Throwable {
        minimalInterceptor.beforeMethod(enhancedInstance, null, regularArgs, argumentsType, null);
        minimalInterceptor.afterMethod(enhancedInstance, null, regularArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat("A trace segment must be created for a non-excluded port", segments.size(), is(1));
        verify(request, org.mockito.Mockito.times(3)).setHeader(anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // Configuration edge cases
    // -----------------------------------------------------------------------

    /**
     * When {@code PROPAGATION_EXCLUDE_PORTS} is cleared (empty string), every
     * port – including 8123 – must be traced normally.
     */
    @Test
    public void whenExcludePortsEmpty_allPortsAreTraced() throws Throwable {
        HttpClient5PluginConfig.Plugin.HttpClient5.PROPAGATION_EXCLUDE_PORTS = "";

        // Use a fresh interceptor so the cache is not pre-populated
        HttpClientDoExecuteInterceptor freshInterceptor = new MinimalClientDoExecuteInterceptor();
        freshInterceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);

        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat("Port 8123 should be traced when exclusion list is empty", segments.size(), is(1));
    }

    /**
     * Multiple ports can be listed: verify that both excluded ports are silently
     * skipped while a non-excluded port is still traced under the same config.
     */
    @Test
    public void multipleExcludedPorts_allSkippedAndNonExcludedStillTraced() throws Throwable {
        HttpClient5PluginConfig.Plugin.HttpClient5.PROPAGATION_EXCLUDE_PORTS = "8123,9200";

        HttpClientDoExecuteInterceptor freshInterceptor = new MinimalClientDoExecuteInterceptor();

        // 8123 – must be excluded
        freshInterceptor.beforeMethod(enhancedInstance, null, clickHouseArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, clickHouseArgs, argumentsType, httpResponse);
        assertThat("Port 8123 should be excluded", segmentStorage.getTraceSegments().size(), is(0));

        // 9200 (Elasticsearch) – must also be excluded
        HttpHost esHost = org.mockito.Mockito.mock(HttpHost.class);
        when(esHost.getHostName()).thenReturn("es-server");
        when(esHost.getSchemeName()).thenReturn("http");
        when(esHost.getPort()).thenReturn(9200);
        Object[] esArgs = new Object[]{esHost, request};

        freshInterceptor = new MinimalClientDoExecuteInterceptor();
        freshInterceptor.beforeMethod(enhancedInstance, null, esArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, esArgs, argumentsType, httpResponse);
        assertThat("Port 9200 should also be excluded", segmentStorage.getTraceSegments().size(), is(0));

        // 8080 (regular service) – must still be traced under the same multi-port config
        freshInterceptor = new MinimalClientDoExecuteInterceptor();
        freshInterceptor.beforeMethod(enhancedInstance, null, regularArgs, argumentsType, null);
        freshInterceptor.afterMethod(enhancedInstance, null, regularArgs, argumentsType, httpResponse);
        assertThat("Non-excluded port 8080 must still produce a trace segment",
                segmentStorage.getTraceSegments().size(), is(1));
    }
}
