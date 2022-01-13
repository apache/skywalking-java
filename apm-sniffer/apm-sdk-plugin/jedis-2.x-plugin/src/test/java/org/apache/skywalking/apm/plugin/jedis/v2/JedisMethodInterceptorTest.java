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

package org.apache.skywalking.apm.plugin.jedis.v2;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
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
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import redis.clients.jedis.Jedis;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class JedisMethodInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    @Mock
    private EnhancedInstance enhancedInstance;

    private JedisMethodInterceptor interceptor;

    private Object[] allArgument;

    private Class[] argumentType;

    @Before
    public void setUp() throws Exception {
        JedisPluginConfig.Plugin.Jedis.TRACE_REDIS_PARAMETERS = true;

        allArgument = new Object[] {
            "OperationKey",
            "OperationValue"
        };
        argumentType = new Class[] {
            String.class,
            String.class
        };

        interceptor = new JedisMethodInterceptor();
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn("127.0.0.1:6379");
    }

    @Test
    public void testIntercept() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getMockSetMethod(), allArgument, argumentType, null);
        interceptor.afterMethod(enhancedInstance, getMockGetMethod(), allArgument, argumentType, null);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertRedisSpan(spans.get(0));
    }

    @Test
    public void testInterceptWithMultiHost() throws Throwable {
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn("127.0.0.1:6379;127.0.0.1:16379;");

        interceptor.beforeMethod(enhancedInstance, getMockSetMethod(), allArgument, argumentType, null);
        interceptor.afterMethod(enhancedInstance, getMockSetMethod(), allArgument, argumentType, null);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertRedisSpan(spans.get(0));
    }

    @Test
    public void testInterceptWithException() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getMockSetMethod(), allArgument, argumentType, null);
        interceptor.handleMethodException(enhancedInstance, getMockSetMethod(), allArgument, argumentType, new RuntimeException());
        interceptor.afterMethod(enhancedInstance, getMockSetMethod(), allArgument, argumentType, null);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertRedisSpan(spans.get(0));

        assertLogData(SpanHelper.getLogs(spans.get(0)));
    }

    private void assertLogData(List<LogDataEntity> logDataEntities) {
        assertThat(logDataEntities.size(), is(1));
        LogDataEntity logData = logDataEntities.get(0);
        Assert.assertThat(logData.getLogs().size(), is(4));
        Assert.assertThat(logData.getLogs().get(0).getValue(), CoreMatchers.<Object>is("error"));
        Assert.assertThat(logData.getLogs()
                                 .get(1)
                                 .getValue(), CoreMatchers.<Object>is(RuntimeException.class.getName()));
        Assert.assertNull(logData.getLogs().get(2).getValue());
        assertNotNull(logData.getLogs().get(3).getValue());
    }

    private void assertRedisSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), is("Jedis/set"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getComponentId(span), is(30));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("Redis"));
        assertThat(tags.get(1).getValue(), is("set OperationKey"));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.CACHE));
    }

    private Method getMockSetMethod() {
        try {
            return Jedis.class.getMethod("set", String.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Method getMockGetMethod() {
        try {
            return Jedis.class.getMethod("get", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

}
