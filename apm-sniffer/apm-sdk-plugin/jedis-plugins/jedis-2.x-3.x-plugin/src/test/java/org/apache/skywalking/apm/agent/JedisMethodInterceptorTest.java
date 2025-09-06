/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.agent;

import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.jedis.v3.JedisMethodInterceptor;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class JedisMethodInterceptorTest {

    private JedisMethodInterceptor interceptor;
    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Before
    public void setUp() {
        interceptor = new JedisMethodInterceptor();
    }

    @Test
    public void testSetStringKey() throws Throwable {
        String methodName = "set";
        String key = "testKey";
        String value = "testValue";
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn("127.0.0.1:6379");
        Object[] arguments = {key, value};
        Class[] argumentTypes = {String.class, value.getClass()};

        interceptor.beforeMethod(enhancedInstance, getJedisSetMethodWithStringKey(), arguments, argumentTypes, null);
        interceptor.afterMethod(enhancedInstance, getJedisSetMethodWithStringKey(), arguments, argumentTypes, null);

        MatcherAssert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        AbstractTracingSpan span = spans.get(0);
        assertThat(span.getOperationName(), startsWith("Jedis/set"));
        assertThat(SpanHelper.getComponentId(span), is(30));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("Redis"));
        assertThat(tags.get(1).getValue(), is(methodName));
        assertThat(tags.get(2).getValue(), is(key));
        assertThat(tags.get(3).getValue(), is("write"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.CACHE));
    }

    private Method getJedisSetMethodWithStringKey() throws Exception {
        return Jedis.class.getMethod("set", String.class, String.class);
    }

    @Test
    public void testSetByteArrayKey() throws Throwable {
        String methodName = "set";
        byte[] key = "testKey".getBytes("utf-8");
        byte[] value = "testValue".getBytes("utf-8");
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn("127.0.0.1:6379");
        Object[] arguments = {key, value};
        Class[] argumentTypes = {String.class, value.getClass()};

        interceptor.beforeMethod(enhancedInstance, getJedisSetMethodWithByteArrayKey(), arguments, argumentTypes, null);
        interceptor.afterMethod(enhancedInstance, getJedisSetMethodWithByteArrayKey(), arguments, argumentTypes, null);

        MatcherAssert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        AbstractTracingSpan span = spans.get(0);
        assertThat(span.getOperationName(), startsWith("Jedis/set"));
        assertThat(SpanHelper.getComponentId(span), is(30));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("Redis"));
        assertThat(tags.get(1).getValue(), is(methodName));
        assertThat(tags.get(2).getValue(), is("testKey"));
        assertThat(tags.get(3).getValue(), is("write"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.CACHE));
    }

    private Method getJedisSetMethodWithByteArrayKey() throws Exception {
        return Jedis.class.getMethod("set", byte[].class, byte[].class);
    }

}