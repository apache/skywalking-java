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

package org.apache.skywalking.apm.plugin.guava.cache;

import static org.hamcrest.CoreMatchers.is;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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

@RunWith(TracingSegmentRunner.class)
public class GuavaCacheInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    private GuavaCacheInterceptor guavaCacheInterceptor;

    private GuavaCacheAllInterceptor guavaCacheAllInterceptor;

    private Object[] operateObjectArguments;

    private Exception exception;

    private Method getAllPresentMethod;
    private Method invalidateAllMethod;
    private Method getMethod;

    private Method invalidateMethod;
    private Method putAllMethod;
    private Method putMethod;
    private Method getIfPresentMethod;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setUp() throws Exception {
        guavaCacheInterceptor = new GuavaCacheInterceptor();
        guavaCacheAllInterceptor = new GuavaCacheAllInterceptor();
        exception = new Exception();
        operateObjectArguments = new Object[]{"dataKey"};
        Class<?> cache = Class.forName("com.google.common.cache.LocalCache$LocalManualCache");
        getAllPresentMethod = cache.getDeclaredMethod("getAllPresent", Iterable.class);
        invalidateAllMethod = cache.getDeclaredMethod("invalidateAll", Iterable.class);
        getMethod = cache.getDeclaredMethod("get", Object.class, Callable.class);
        invalidateMethod = cache.getDeclaredMethod("invalidate", Object.class);
        putAllMethod = cache.getDeclaredMethod("putAll", Map.class);
        putMethod = cache.getDeclaredMethod("put", Object.class, Object.class);
        getIfPresentMethod = cache.getDeclaredMethod("getIfPresent", Object.class);

    }

    @Test
    public void assertGetAllPresentSuccess() throws Throwable {
        guavaCacheAllInterceptor.beforeMethod(null, getAllPresentMethod, null, null, null);
        guavaCacheAllInterceptor.handleMethodException(null, getAllPresentMethod, null, null, exception);
        guavaCacheAllInterceptor.afterMethod(null, getAllPresentMethod, null, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void assertInvalidAllSuccess() throws Throwable {
        guavaCacheAllInterceptor.beforeMethod(null, invalidateAllMethod, null, null, null);
        guavaCacheAllInterceptor.handleMethodException(null, invalidateAllMethod, null, null, exception);
        guavaCacheAllInterceptor.afterMethod(null, invalidateAllMethod, null, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void assertGetSuccess() throws Throwable {
        guavaCacheInterceptor.beforeMethod(null, getMethod, operateObjectArguments, null, null);
        guavaCacheInterceptor.handleMethodException(null, getMethod, operateObjectArguments, null, exception);
        guavaCacheInterceptor.afterMethod(null, getMethod, operateObjectArguments, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void assertInvalidateSuccess() throws Throwable {
        guavaCacheInterceptor.beforeMethod(null, invalidateMethod, operateObjectArguments, null, null);
        guavaCacheInterceptor.handleMethodException(null, invalidateMethod, operateObjectArguments, null, exception);
        guavaCacheInterceptor.afterMethod(null, invalidateMethod, operateObjectArguments, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void assertPutAllMethodSuccess() throws Throwable {
        guavaCacheAllInterceptor.beforeMethod(null, putAllMethod, null, null, null);
        guavaCacheAllInterceptor.handleMethodException(null, putAllMethod, null, null, exception);
        guavaCacheAllInterceptor.afterMethod(null, putAllMethod, null, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void assertPutMethodSuccess() throws Throwable {
        guavaCacheInterceptor.beforeMethod(null, putMethod, operateObjectArguments, null, null);
        guavaCacheInterceptor.handleMethodException(null, putMethod, operateObjectArguments, null, exception);
        guavaCacheInterceptor.afterMethod(null, putMethod, operateObjectArguments, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }

    @Test
    public void assertGetIfPresentMethodSuccess() throws Throwable {
        guavaCacheInterceptor.beforeMethod(null, getIfPresentMethod, operateObjectArguments, null, null);
        guavaCacheInterceptor.handleMethodException(null, getIfPresentMethod, operateObjectArguments, null, exception);
        guavaCacheInterceptor.afterMethod(null, getIfPresentMethod, operateObjectArguments, null, null);
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegments.size(), is(1));
    }
}
