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

package org.apache.skywalking.apm.plugin.lettuce.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;

@RunWith(TracingSegmentRunner.class)
public class RedisChannelWriterInterceptorTest {

    public static final String PEER = "192.168.1.12:6379";

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MockInstance mockRedisChannelWriterInstance;

    private RedisChannelWriterInterceptor interceptor;

    private RedisCommandCompleteMethodInterceptor redisCommandCompleteMethodInterceptor;

    private static class MockInstance implements EnhancedInstance {
        private Object object;

        @Override
        public Object getSkyWalkingDynamicField() {
            return object;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.object = value;
        }
    }

    private static class MockRedisCommand<K, V, T> extends Command<K, V, T>
        implements EnhancedInstance {
        private Object object;

        public MockRedisCommand(ProtocolKeyword type, CommandOutput<K, V, T> output) {
            super(type, output);
        }

        public MockRedisCommand(ProtocolKeyword type, CommandOutput<K, V, T> output,
            CommandArgs<K, V> args) {
            super(type, output, args);
        }

        @Override
        public Object getSkyWalkingDynamicField() {
            return object;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.object = value;
        }
    }

    private static class CancelledWhileSpanIsCreatedCommand<K, V, T> extends MockRedisCommand<K, V, T> {
        public CancelledWhileSpanIsCreatedCommand(ProtocolKeyword type, CommandOutput<K, V, T> output) {
            super(type, output);
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            super.setSkyWalkingDynamicField(value);
            if (value != null) {
                cancel();
            }
        }
    }

    @Before
    public void setUp() {
        LettucePluginConfig.Plugin.Lettuce.TRACE_REDIS_PARAMETERS = true;
        mockRedisChannelWriterInstance = new MockInstance();
        mockRedisChannelWriterInstance.setSkyWalkingDynamicField(PEER);
        interceptor = new RedisChannelWriterInterceptor();
        redisCommandCompleteMethodInterceptor = new RedisCommandCompleteMethodInterceptor();
    }

    @Test
    public void testInterceptor() {
        CommandArgs<?, ?> args = new CommandArgs<>(new ByteArrayCodec()).addKey("name".getBytes());
        MockRedisCommand<?, ?, ?> redisCommand = new MockRedisCommand<>(CommandType.SET, null, args);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{redisCommand}, null, null);
        interceptor.afterMethod(mockRedisChannelWriterInstance, null, null, null, null);
        redisCommandCompleteMethodInterceptor.afterMethod(redisCommand, null, null, null, null);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertNotNull(spans);
        assertThat(spans.size(), is(1));
        assertThat(spans.get(0).getOperationName(), is("Lettuce/UNKNOWN"));
        assertThat(spans.get(0).isExit(), is(true));
        assertThat(SpanHelper.getComponentId(spans.get(0)), is(57));
        List<TagValuePair> tags = SpanHelper.getTags(spans.get(0));
        assertThat(tags.get(0).getValue(), is("Redis"));
        assertThat(SpanHelper.getLayer(spans.get(0)), CoreMatchers.is(SpanLayer.CACHE));
        assertThat(SpanHelper.getPeer(spans.get(0)), is(PEER));
    }

    @Test
    public void testDoneCommandHasNoSpan() {
        MockRedisCommand<?, ?, ?> redisCommand = new MockRedisCommand<>(CommandType.SET, null);
        redisCommand.cancel();
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{redisCommand}, null, null);

        assertNull(redisCommand.getSkyWalkingDynamicField());
        assertThat(segmentStorage.getTraceSegments().size(), is(0));
    }

    @Test
    public void testCommandCancelledWhileSpanIsCreatedHasItsSpanFinished() {
        CancelledWhileSpanIsCreatedCommand<?, ?, ?> redisCommand = new CancelledWhileSpanIsCreatedCommand<>(CommandType.SET, null);
        // The cancellation interceptor found no span, and the command is done by the time the writer has set the span.
        new RedisCommandCancelMethodInterceptor().afterMethod(redisCommand, null, null, null, null);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{redisCommand}, null, null);

        assertNull(redisCommand.getSkyWalkingDynamicField());
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));
        assertThat(spans.size(), is(1));
        assertThat(SpanHelper.getErrorOccurred(spans.get(0)), is(true));
    }

    @Test
    public void testSpanIsTakenByOneTerminalInterceptor() {
        MockRedisCommand<?, ?, ?> redisCommand = new MockRedisCommand<>(CommandType.SET, null);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{redisCommand}, null, null);
        Object enhanceInfo = redisCommand.getSkyWalkingDynamicField();

        redisCommandCompleteMethodInterceptor.afterMethod(redisCommand, null, null, null, null);
        // A concurrent cancellation read the enhance info before the completion cleared it.
        redisCommand.setSkyWalkingDynamicField(enhanceInfo);
        new RedisCommandCancelMethodInterceptor().afterMethod(redisCommand, null, null, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        assertThat(SpanHelper.getErrorOccurred(SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0)).get(0)), is(false));
    }

    @Test
    public void testGetSpanCarrierCommand() throws Exception {
        Command<?, ?, ?> command = new Command<>(CommandType.SET, null, null);
        Method getSpanCarrierCommandMethod = interceptor.getClass()
            .getDeclaredMethod("getSpanCarrierCommand", Object.class);
        getSpanCarrierCommandMethod.setAccessible(true);

        RedisCommand<?, ?, ?> redisCommand =
            (RedisCommand<?, ?, ?>) getSpanCarrierCommandMethod.invoke(interceptor, command);
        assertEquals(command, redisCommand);
        List<RedisCommand<?, ?, ?>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new Command<>(CommandType.SET, null, null));
        }
        list.add(command);
        RedisCommand<?, ?, ?> last =
            (RedisCommand<?, ?, ?>) getSpanCarrierCommandMethod.invoke(interceptor, list);
        assertEquals(command, last);
        Command<?, ?, ?> cancelled = new Command<>(CommandType.SET, null, null);
        cancelled.cancel();
        list.add(cancelled);
        RedisCommand<?, ?, ?> lastNotDone =
            (RedisCommand<?, ?, ?>) getSpanCarrierCommandMethod.invoke(interceptor, list);
        assertEquals(command, lastNotDone);
        list.remove(cancelled);
        RedisCommand<?, ?, ?> nullValue1 =
            (RedisCommand<?, ?, ?>) getSpanCarrierCommandMethod.invoke(interceptor, (Object) null);
        assertNull(nullValue1);
        list.add(null);
        RedisCommand<?, ?, ?> nullValue2 =
            (RedisCommand<?, ?, ?>) getSpanCarrierCommandMethod.invoke(interceptor, list);
        assertNull(nullValue2);
    }
}
