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

package org.apache.skywalking.apm.plugin.caffeine.v3;

import com.github.benmanes.caffeine.cache.Expiry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;

@RunWith(TracingSegmentRunner.class)
public class CaffeineInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    private CaffeineIterableInterceptor caffeineIterableInterceptor;
    private CaffeineMapInterceptor caffeineMapInterceptor;
    private CaffeineStringInterceptor caffeineStringInterceptor;
    private Object[] operateObjectArguments;

    private Exception exception;

    private Method getAllPresentMethod;
    private Method getIfPresentMethod;
    private Method computeIfAbsentMethod;

    private Method putMethod;
    private Method putAllMethod;
    private Method removeMethod;
    private Method cleanMethod;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setUp() throws Exception {
        caffeineIterableInterceptor = new CaffeineIterableInterceptor();
        caffeineMapInterceptor = new CaffeineMapInterceptor();
        caffeineStringInterceptor = new CaffeineStringInterceptor();
        exception = new Exception();
        operateObjectArguments = new Object[] {"dataKey"};
        Class<?> cache = Class.forName("com.github.benmanes.caffeine.cache.BoundedLocalCache");
        getAllPresentMethod = cache.getDeclaredMethod("getAllPresent", Iterable.class);
        getIfPresentMethod = cache.getDeclaredMethod("getIfPresent", Object.class, boolean.class);
        computeIfAbsentMethod = cache.getDeclaredMethod(
            "computeIfAbsent", Object.class, Function.class, boolean.class, boolean.class);
        putMethod = cache.getDeclaredMethod("put", Object.class, Object.class, Expiry.class, boolean.class);
        putAllMethod = cache.getDeclaredMethod("putAll", Map.class);
        removeMethod = cache.getDeclaredMethod("remove", Object.class);
        cleanMethod = cache.getDeclaredMethod("clear");
    }

    @Test
    public void testGetAllPresentSuccess() throws Throwable {
        caffeineIterableInterceptor.beforeMethod(null, getAllPresentMethod, null, null, null);
        caffeineIterableInterceptor.handleMethodException(null, getAllPresentMethod, null, null, exception);
        caffeineIterableInterceptor.afterMethod(null, getAllPresentMethod, null, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testGetIfPresentSuccess() throws Throwable {
        caffeineStringInterceptor.beforeMethod(null, getIfPresentMethod, operateObjectArguments, null, null);
        caffeineStringInterceptor.handleMethodException(
            null, getIfPresentMethod, operateObjectArguments, null, exception);
        caffeineStringInterceptor.afterMethod(null, getIfPresentMethod, operateObjectArguments, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testComputeIfAbsentMethodSuccess() throws Throwable {
        caffeineStringInterceptor.beforeMethod(null, computeIfAbsentMethod, null, null, null);
        caffeineStringInterceptor.handleMethodException(null, computeIfAbsentMethod, null, null, exception);
        caffeineStringInterceptor.afterMethod(null, computeIfAbsentMethod, null, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testPutMethodSuccess() throws Throwable {
        caffeineStringInterceptor.beforeMethod(null, putMethod, operateObjectArguments, null, null);
        caffeineStringInterceptor.handleMethodException(null, putMethod, operateObjectArguments, null, exception);
        caffeineStringInterceptor.afterMethod(null, putMethod, operateObjectArguments, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testPutAllMethodSuccess() throws Throwable {
        caffeineMapInterceptor.beforeMethod(null, putAllMethod, null, null, null);
        caffeineMapInterceptor.handleMethodException(null, putAllMethod, null, null, exception);
        caffeineMapInterceptor.afterMethod(null, putAllMethod, null, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testRemoveMethodSuccess() throws Throwable {
        caffeineStringInterceptor.beforeMethod(null, removeMethod, operateObjectArguments, null, null);
        caffeineStringInterceptor.handleMethodException(null, removeMethod, operateObjectArguments, null, exception);
        caffeineStringInterceptor.afterMethod(null, removeMethod, operateObjectArguments, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void testClearMethodSuccess() throws Throwable {
        caffeineStringInterceptor.beforeMethod(null, cleanMethod, null, null, null);
        caffeineStringInterceptor.handleMethodException(null, cleanMethod, null, null, exception);
        caffeineStringInterceptor.afterMethod(null, cleanMethod, null, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }
}
