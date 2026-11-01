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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.status.StatusCheckService;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
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

    private static class IgnoredException extends RuntimeException {
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
    public void testBatchSpanIsFinishedByItsLastUnfinishedCommand() {
        MockRedisCommand<?, ?, ?> first = new MockRedisCommand<>(CommandType.SET, null);
        MockRedisCommand<?, ?, ?> last = new MockRedisCommand<>(CommandType.SET, null);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{Arrays.asList(first, last)}, null, null);

        // The last command is cancelled while the first one still waits for its reply.
        last.cancel();
        new RedisCommandCancelMethodInterceptor().afterMethod(last, null, null, null, null);
        assertThat(segmentStorage.getTraceSegments().size(), is(0));

        redisCommandCompleteMethodInterceptor.afterMethod(first, null, null, null, null);
        AbstractTracingSpan span = getBatchSpan();
        assertThat(SpanHelper.getErrorOccurred(span), is(true));
        assertThat(SpanHelper.getPeer(span), is(PEER));
    }

    @Test
    public void testErrorReplyOfEarlierCommandFailsBatchSpan() {
        MockRedisCommand<String, String, String> errorReply = new MockRedisCommand<>(CommandType.INCR, new StatusOutput<>(StringCodec.UTF8));
        MockRedisCommand<?, ?, ?> last = new MockRedisCommand<>(CommandType.SET, null);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{Arrays.asList(errorReply, last)}, null, null);

        // A reactive command is completed without an exception on an error reply.
        errorReply.getOutput().setError("ERR value is not an integer or out of range");
        redisCommandCompleteMethodInterceptor.afterMethod(errorReply, null, null, null, null);
        assertThat(segmentStorage.getTraceSegments().size(), is(0));
        redisCommandCompleteMethodInterceptor.afterMethod(last, null, null, null, null);

        AbstractTracingSpan span = getBatchSpan();
        assertThat(SpanHelper.getErrorOccurred(span), is(true));
        List<LogDataEntity> logs = SpanHelper.getLogs(span);
        assertThat(logs.size(), is(1));
        assertThat(logs.get(0).getLogs().get(0).getValue(), is("error"));
        assertThat(logs.get(0).getLogs().get(1).getValue(), is("ERR value is not an integer or out of range"));
    }

    @Test
    public void testFailuresOfBatchCommandsAreLoggedOnce() {
        MockRedisCommand<?, ?, ?> first = new MockRedisCommand<>(CommandType.SET, null);
        MockRedisCommand<?, ?, ?> last = new MockRedisCommand<>(CommandType.SET, null);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{Arrays.asList(first, last)}, null, null);

        // The connection is closed while both commands wait for their replies.
        new RedisCommandCompleteExceptionallyMethodInterceptor().afterMethod(
            first, null, new Object[]{new IllegalStateException("closed")}, null, null);
        new RedisCommandCompleteExceptionallyMethodInterceptor().afterMethod(
            last, null, new Object[]{new IllegalStateException("closed")}, null, null);

        AbstractTracingSpan span = getBatchSpan();
        assertThat(SpanHelper.getErrorOccurred(span), is(true));
        assertThat(SpanHelper.getLogs(span).size(), is(1));
    }

    @Test
    public void testIgnoredFirstExceptionDoesNotHideLaterFailure() throws Throwable {
        StatusCheckService statusCheckService = ServiceManager.INSTANCE.findService(StatusCheckService.class);
        Config.StatusCheck.IGNORED_EXCEPTIONS = IgnoredException.class.getName();
        statusCheckService.prepare();
        try {
            MockRedisCommand<?, ?, ?> first = new MockRedisCommand<>(CommandType.SET, null);
            MockRedisCommand<?, ?, ?> last = new MockRedisCommand<>(CommandType.SET, null);
            interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{Arrays.asList(first, last)}, null, null);

            new RedisCommandCompleteExceptionallyMethodInterceptor().afterMethod(
                first, null, new Object[]{new IgnoredException()}, null, null);
            new RedisCommandCompleteExceptionallyMethodInterceptor().afterMethod(
                last, null, new Object[]{new IllegalStateException("failed")}, null, null);

            AbstractTracingSpan span = getBatchSpan();
            assertThat(SpanHelper.getErrorOccurred(span), is(true));
            assertThat(SpanHelper.getLogs(span).size(), is(1));
        } finally {
            Config.StatusCheck.IGNORED_EXCEPTIONS = "";
            statusCheckService.prepare();
        }
    }

    @Test
    public void testCommandCancelledWhileBatchIsTracedLeavesOneSpan() {
        MockRedisCommand<?, ?, ?> first = new MockRedisCommand<>(CommandType.SET, null);
        CancelledWhileSpanIsCreatedCommand<?, ?, ?> last = new CancelledWhileSpanIsCreatedCommand<>(CommandType.SET, null);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{Arrays.asList(first, last)}, null, null);

        assertNull(last.getSkyWalkingDynamicField());
        assertThat(segmentStorage.getTraceSegments().size(), is(0));
        redisCommandCompleteMethodInterceptor.afterMethod(first, null, null, null, null);
        assertThat(SpanHelper.getErrorOccurred(getBatchSpan()), is(true));
    }

    @Test
    public void testDelegateWriterKeepsSpanOfTracedBatch() {
        MockInstance outerWriter = new MockInstance();
        MockRedisCommand<?, ?, ?> first = new MockRedisCommand<>(CommandType.SET, null);
        MockRedisCommand<?, ?, ?> last = new MockRedisCommand<>(CommandType.SET, null);
        List<RedisCommand<?, ?, ?>> batch = Arrays.asList(first, last);
        interceptor.beforeMethod(outerWriter, null, new Object[]{batch}, null, null);

        // The last command is cancelled before the outer writer delegates the write.
        last.cancel();
        new RedisCommandCancelMethodInterceptor().afterMethod(last, null, null, null, null);
        interceptor.beforeMethod(mockRedisChannelWriterInstance, null, new Object[]{batch}, null, null);

        redisCommandCompleteMethodInterceptor.afterMethod(first, null, null, null, null);
        assertThat(SpanHelper.getPeer(getBatchSpan()), is(PEER));
    }

    @Test
    public void testGetSpanCarrierCommand() {
        Command<?, ?, ?> command = new Command<>(CommandType.SET, null, null);
        assertEquals(command, RedisChannelWriterInterceptor.getSpanCarrierCommand(command));
        assertEquals(command, RedisChannelWriterInterceptor.getSpanCarrierCommand(new AsyncCommand<>(command)));
        assertNull(RedisChannelWriterInterceptor.getSpanCarrierCommand(null));
        assertNull(RedisChannelWriterInterceptor.getSpanCarrierCommand(Collections.singletonList(command)));
    }

    private AbstractTracingSpan getBatchSpan() {
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0));
        assertThat(spans.size(), is(1));
        assertThat(spans.get(0).getOperationName(), is("Lettuce/BATCH_WRITE"));
        return spans.get(0);
    }
}
