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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.protocol.TracedCommand;
import io.lettuce.core.tracing.TraceContext;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

@RunWith(TracingSegmentRunner.class)
public class RedisReactiveCommandSpanTest {

    private static final String PEER = "192.168.1.12:6379";

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private final RedisCommandCompleteMethodInterceptor completeInterceptor = new RedisCommandCompleteMethodInterceptor();
    private final RedisCommandCancelMethodInterceptor cancelInterceptor = new RedisCommandCancelMethodInterceptor();
    private final RedisCommandCompleteExceptionallyMethodInterceptor completeExceptionallyInterceptor =
        new RedisCommandCompleteExceptionallyMethodInterceptor();

    private MockCommand command;
    private MockTracedCommand tracedCommand;
    private MockInstance channelWriter;
    private MockInstance subscription;

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

    private static class MockCommand extends Command<String, String, String> implements EnhancedInstance {
        private Object object;

        MockCommand() {
            super(CommandType.SET, null);
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

    private static class MockTracedCommand extends TracedCommand<String, String, String> implements EnhancedInstance {
        private Object object;

        MockTracedCommand(MockCommand command) {
            super(command, TraceContext.EMPTY);
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

    private static class MockSubscriber implements CoreSubscriber<Object> {
        private final ContextSnapshot snapshot;

        MockSubscriber(ContextSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Context currentContext() {
            return Context.of("SKYWALKING_CONTEXT_SNAPSHOT", snapshot);
        }

        @Override
        public void onSubscribe(Subscription s) {
        }

        @Override
        public void onNext(Object o) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }
    }

    @Before
    public void setUp() {
        command = new MockCommand();
        tracedCommand = new MockTracedCommand(command);
        channelWriter = new MockInstance();
        channelWriter.setSkyWalkingDynamicField(PEER);
        subscription = new MockInstance();
    }

    @Test
    public void snapshotOfDecoratedCommandIsContinuedOnSpanCarrierCommand() {
        subscribe(tracedCommand);
        write(tracedCommand);

        assertThat(tracedCommand.getSkyWalkingDynamicField(), nullValue());

        completeInterceptor.afterMethod(command, null, null, null, null);
        // Mono#next cancels the subscription after the command has been completed.
        completeInterceptor.afterMethod(tracedCommand, null, null, null, null);
        cancelInterceptor.afterMethod(tracedCommand, null, null, null, null);
        cancelInterceptor.afterMethod(command, null, null, null, null);

        assertThat(command.getSkyWalkingDynamicField(), nullValue());
        assertSpansOfContinuedCommand();
    }

    @Test
    public void commandWithoutDecoratorStillContinuesSnapshot() {
        subscribe(command);
        assertThat(subscription.getSkyWalkingDynamicField(), sameInstance((Object) command));

        write(command);
        completeInterceptor.afterMethod(command, null, null, null, null);

        assertSpansOfContinuedCommand();
    }

    @Test
    public void commandCancelledBeforeWriteHasNoSpanToFinish() {
        subscribe(command);
        Throwable error = new IllegalStateException("cancelled");

        cancelInterceptor.afterMethod(command, null, null, null, null);
        cancelInterceptor.handleMethodException(command, null, null, null, error);
        completeInterceptor.afterMethod(command, null, null, null, null);
        completeInterceptor.handleMethodException(command, null, null, null, error);
        completeExceptionallyInterceptor.afterMethod(command, null, new Object[]{error}, null, null);
        completeExceptionallyInterceptor.handleMethodException(command, null, null, null, error);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        assertThat(SegmentHelper.getSpans(segmentStorage.getTraceSegments().get(0)).size(), is(1));
    }

    private void subscribe(RedisCommand<?, ?, ?> redisCommand) {
        ContextManager.createEntrySpan("/redis", null);
        ContextSnapshot snapshot = ContextManager.capture();
        ContextManager.stopSpan();

        new RedisSubscriptionConstructorInterceptor().onConstruct(subscription, new Object[]{null, redisCommand});
        new RedisSubscriptionSubscribeMethodInterceptor().beforeMethod(
            subscription, null, new Object[]{new MockSubscriber(snapshot)}, null, null);
    }

    private void write(Object redisCommand) {
        new RedisChannelWriterInterceptor().beforeMethod(channelWriter, null, new Object[]{redisCommand}, null, null);
    }

    private void assertSpansOfContinuedCommand() {
        List<TraceSegment> segments = segmentStorage.getTraceSegments();
        assertThat(segments.size(), is(2));
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segments.get(1));
        assertThat(spans.size(), is(2));
        assertThat(spans.get(0).getOperationName(), is("Lettuce/UNKNOWN"));
        assertThat(spans.get(0).isExit(), is(true));
        assertThat(spans.get(1).getOperationName(), is("RedisReactive/local"));
        assertThat(segments.get(1).getRelatedGlobalTrace().getId(), is(segments.get(0).getRelatedGlobalTrace().getId()));
    }
}
