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

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v412x;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.spring.cloud.gateway.v4x.define.EnhanceObjectCache;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import reactor.netty.http.client.HttpClient;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(TracingSegmentRunner.class)
public class NettyRoutingGetHttpClientV412InterceptorTest {
    private final static String ENTRY_OPERATION_NAME = "/get";

    private final NettyRoutingGetHttpClientV412Interceptor interceptor = new NettyRoutingGetHttpClientV412Interceptor();

    /**
     * Stands for the single <code>NettyRoutingFilter#httpClient</code> bean, which
     * <code>NettyRoutingFilter#getHttpClient</code> returns to every request.
     */
    private final HttpClient sharedHttpClient = mockHttpClient();

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Test
    public void testSnapshotIsHeldByADerivedClientAndNotByTheSharedOne() throws Throwable {
        final HttpClient derivedHttpClient = mockHttpClient();
        when(sharedHttpClient.headers(any())).thenReturn(derivedHttpClient);

        final Object ret = getHttpClientWithinARequest(sharedHttpClient);

        assertSame(derivedHttpClient, ret);
        assertNotNull(snapshotOf(derivedHttpClient));
        // The shared bean is reused by every request, so it must never hold a request scoped snapshot.
        verify((EnhancedInstance) sharedHttpClient, never()).setSkyWalkingDynamicField(any());
    }

    @Test
    public void testConcurrentRequestsDoNotShareTheSnapshot() throws Throwable {
        final HttpClient firstDerivedHttpClient = mockHttpClient();
        final HttpClient secondDerivedHttpClient = mockHttpClient();
        when(sharedHttpClient.headers(any())).thenReturn(firstDerivedHttpClient, secondDerivedHttpClient);

        final Object firstRet = getHttpClientWithinARequest(sharedHttpClient);
        final Object secondRet = getHttpClientWithinARequest(sharedHttpClient);

        assertSame(firstDerivedHttpClient, firstRet);
        assertSame(secondDerivedHttpClient, secondRet);
        assertNotEquals(
            snapshotOf(firstDerivedHttpClient).getTraceId().getId(),
            snapshotOf(secondDerivedHttpClient).getTraceId().getId()
        );
        verify((EnhancedInstance) sharedHttpClient, never()).setSkyWalkingDynamicField(any());
    }

    @Test
    public void testWithContextNotActive() throws Throwable {
        final Object ret = interceptor.afterMethod(null, null, null, null, sharedHttpClient);

        assertSame(sharedHttpClient, ret);
        // Nothing to propagate, so not even a client is derived.
        verify(sharedHttpClient, never()).headers(any());
        verify((EnhancedInstance) sharedHttpClient, never()).setSkyWalkingDynamicField(any());
    }

    private Object getHttpClientWithinARequest(final HttpClient httpClient) throws Throwable {
        final AbstractSpan entrySpan = ContextManager.createEntrySpan(ENTRY_OPERATION_NAME, null);
        entrySpan.setLayer(SpanLayer.HTTP);
        entrySpan.setComponent(ComponentsDefine.SPRING_WEBFLUX);
        try {
            return interceptor.afterMethod(null, null, null, null, httpClient);
        } finally {
            ContextManager.stopSpan();
        }
    }

    private ContextSnapshot snapshotOf(final HttpClient httpClient) {
        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify((EnhancedInstance) httpClient).setSkyWalkingDynamicField(captor.capture());
        return ((EnhanceObjectCache) captor.getValue()).getContextSnapshot();
    }

    private static HttpClient mockHttpClient() {
        return mock(HttpClient.class, withSettings().extraInterfaces(EnhancedInstance.class));
    }
}
