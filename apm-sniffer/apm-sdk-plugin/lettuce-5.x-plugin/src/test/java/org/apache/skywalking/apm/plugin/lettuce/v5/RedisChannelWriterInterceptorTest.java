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

package org.apache.skywalking.apm.plugin.lettuce.v5;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
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
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class RedisChannelWriterInterceptorTest {

    public static final String PEER = "192.168.1.12:6379";

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

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

    private static class MockRedisCommand<K, V, T> extends Command<K, V, T> implements EnhancedInstance {
        private Object object;

        public MockRedisCommand(ProtocolKeyword type, CommandOutput<K, V, T> output) {
            super(type, output);
        }

        public MockRedisCommand(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
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
        CommandArgs<?, ?> args = new CommandArgs<>(new ByteArrayCodec()).addKey("name".getBytes()).addValue("Tom".getBytes());
        MockRedisCommand<?, ?, ?> redisCommand = new MockRedisCommand<>(CommandType.SET, null, args);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{redisCommand}, null, null);
        interceptor.afterMethod(mockRedisChannelWriterInstance, null, null, null, null);
        redisCommandCompleteMethodInterceptor.afterMethod(redisCommand, null, null, null, null);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertNotNull(spans);
        assertThat(spans.size(), is(1));
        assertThat(spans.get(0).getOperationName(), is("Lettuce/SET"));
        assertThat(spans.get(0).isExit(), is(true));
        assertThat(SpanHelper.getComponentId(spans.get(0)), is(57));
        List<TagValuePair> tags = SpanHelper.getTags(spans.get(0));
        assertThat(tags.get(0).getValue(), is("Redis"));
        assertThat(tags.get(1).getValue(), CoreMatchers.containsString("Tom"));
        assertThat(SpanHelper.getLayer(spans.get(0)), CoreMatchers.is(SpanLayer.CACHE));
        assertThat(SpanHelper.getPeer(spans.get(0)), is(PEER));
    }

    @Test
    public void testGetSpanCarrierCommand() throws Exception {
        Command<?, ?, ?> command = new Command<>(CommandType.SET, null, null);
        RedisCommand<?, ?, ?> redisCommand = Whitebox.invokeMethod(interceptor, "getSpanCarrierCommand", command);
        assertEquals(command, redisCommand);
        List<RedisCommand<?, ?, ?>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new Command<>(CommandType.SET, null, null));
        }
        list.add(command);
        RedisCommand<?, ?, ?> last = Whitebox.invokeMethod(interceptor, "getSpanCarrierCommand", list);
        assertEquals(command, last);
        RedisCommand<?, ?, ?> nullValue1 = Whitebox.invokeMethod(interceptor, "getSpanCarrierCommand", (Object) null);
        assertNull(nullValue1);
        list.add(null);
        RedisCommand<?, ?, ?> nullValue2 = Whitebox.invokeMethod(interceptor, "getSpanCarrierCommand", list);
        assertNull(nullValue2);
    }
}
