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
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.spring.cloud.gateway.v4x.define.EnhanceObjectCache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(TracingSegmentRunner.class)
public class HttpClientFinalizerUriV412InterceptorTest {

    private final static String URI = "http://localhost:8080/get";
    private final static String ENTRY_OPERATION_NAME = "/get";
    private final HttpClientFinalizerUriV412Interceptor uriInterceptor = new HttpClientFinalizerUriV412Interceptor();
    private final EnhancedInstance enhancedInstance = new EnhancedInstance() {
        private EnhanceObjectCache enhanceObjectCache;

        @Override
        public Object getSkyWalkingDynamicField() {
            return enhanceObjectCache;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.enhanceObjectCache = (EnhanceObjectCache) value;
        }
    };
    private final EnhancedInstance retEnhancedInstance = new EnhancedInstance() {
        private EnhanceObjectCache enhanceObjectCache;
        @Override
        public Object getSkyWalkingDynamicField() {
            return enhanceObjectCache;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.enhanceObjectCache = (EnhanceObjectCache) value;
        }
    };

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    private AbstractSpan entrySpan;

    @Before
    public void setUp() throws Exception {
        entrySpan = ContextManager.createEntrySpan(ENTRY_OPERATION_NAME, null);
        entrySpan.setLayer(SpanLayer.HTTP);
        entrySpan.setComponent(ComponentsDefine.SPRING_WEBFLUX);
    }

    @Test
    public void testWithDynamicFieldNull() throws Throwable {
        enhancedInstance.setSkyWalkingDynamicField(null);
        retEnhancedInstance.setSkyWalkingDynamicField(null);
        uriInterceptor.afterMethod(enhancedInstance, null, null, null, retEnhancedInstance);
        assertNull(retEnhancedInstance.getSkyWalkingDynamicField());
        final List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertEquals(traceSegments.size(), 0);
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
    }

    @Test
    public void testWithRetDynamicFieldNotNull() throws Throwable {
        enhancedInstance.setSkyWalkingDynamicField(null);
        EnhanceObjectCache enhanceObjectCache2 = new EnhanceObjectCache();
        retEnhancedInstance.setSkyWalkingDynamicField(enhanceObjectCache2);

        uriInterceptor.afterMethod(enhancedInstance, null, new Object[]{URI}, null, retEnhancedInstance);
        Object retEnhancedInstanceSkyWalkingDynamicField = retEnhancedInstance.getSkyWalkingDynamicField();
        assertNotNull(retEnhancedInstanceSkyWalkingDynamicField);
        assertTrue(retEnhancedInstanceSkyWalkingDynamicField instanceof EnhanceObjectCache);
        EnhanceObjectCache retEnhanceObjectCache = (EnhanceObjectCache) retEnhancedInstanceSkyWalkingDynamicField;
        assertNull(retEnhanceObjectCache.getContextSnapshot());
        assertNotNull(retEnhanceObjectCache.getUrl());
        assertEquals(URI, retEnhanceObjectCache.getUrl());
        final List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertEquals(traceSegments.size(), 0);
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
    }

    @Test
    public void testWithDynamicFieldNotNull() throws Throwable {
        ContextSnapshot contextSnapshot = ContextManager.capture();
        EnhanceObjectCache enhanceObjectCache = new EnhanceObjectCache();
        enhanceObjectCache.setContextSnapshot(contextSnapshot);
        enhancedInstance.setSkyWalkingDynamicField(enhanceObjectCache);
        EnhanceObjectCache enhanceObjectCache2 = new EnhanceObjectCache();
        retEnhancedInstance.setSkyWalkingDynamicField(enhanceObjectCache2);

        uriInterceptor.afterMethod(enhancedInstance, null, new Object[]{URI}, null, retEnhancedInstance);
        Object retEnhancedInstanceSkyWalkingDynamicField = retEnhancedInstance.getSkyWalkingDynamicField();
        assertNotNull(retEnhancedInstanceSkyWalkingDynamicField);
        assertTrue(retEnhancedInstanceSkyWalkingDynamicField instanceof EnhanceObjectCache);
        EnhanceObjectCache retEnhanceObjectCache = (EnhanceObjectCache) retEnhancedInstanceSkyWalkingDynamicField;
        assertNotNull(retEnhanceObjectCache.getContextSnapshot());
        assertTrue(contextSnapshot == retEnhanceObjectCache.getContextSnapshot());
        assertNotNull(retEnhanceObjectCache.getUrl());
        assertEquals(URI, retEnhanceObjectCache.getUrl());
        final List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertEquals(traceSegments.size(), 0);
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
    }

    @Test
    public void testWithRetDynamicFieldNull() throws Throwable {
        ContextSnapshot contextSnapshot = ContextManager.capture();
        EnhanceObjectCache enhanceObjectCache = new EnhanceObjectCache();
        enhanceObjectCache.setContextSnapshot(contextSnapshot);
        enhancedInstance.setSkyWalkingDynamicField(enhanceObjectCache);
        retEnhancedInstance.setSkyWalkingDynamicField(null);

        uriInterceptor.afterMethod(enhancedInstance, null, new Object[]{URI}, null, retEnhancedInstance);
        Object retEnhancedInstanceSkyWalkingDynamicField = retEnhancedInstance.getSkyWalkingDynamicField();
        assertNull(retEnhancedInstanceSkyWalkingDynamicField);
        assertNotNull(enhancedInstance.getSkyWalkingDynamicField());
        assertTrue(enhancedInstance.getSkyWalkingDynamicField() instanceof EnhanceObjectCache);
        EnhanceObjectCache objInstObjectCache = (EnhanceObjectCache) enhancedInstance.getSkyWalkingDynamicField();
        assertTrue(contextSnapshot == objInstObjectCache.getContextSnapshot());
        assertNull(objInstObjectCache.getUrl());
        final List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertEquals(traceSegments.size(), 0);
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
    }
}
