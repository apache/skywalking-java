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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.FutureCallbackWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(TracingSegmentRunner.class)
public class FutureCallbackWrapperTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FutureCallback<String> delegate;

    @Before
    public void setUp() {
        ServiceManager.INSTANCE.boot();
    }

    private AsyncExitSpan createStartedExitSpan() {
        AsyncExitSpan exitSpan = new AsyncExitSpan(
                new HttpHost("http", "127.0.0.1", 8080));

        AbstractSpan requestSpan = ContextManager.createExitSpan(
                "/hello",
                new ContextCarrier(),
                "127.0.0.1:8080");

        exitSpan.start(requestSpan);
        requestSpan.prepareForAsync();
        ContextManager.stopSpan(requestSpan);

        return exitSpan;
    }

    @Test
    public void completedKeepsCallerSpanActive() {
        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);
        AsyncExitSpan exitSpan = createStartedExitSpan();

        new FutureCallbackWrapper<>(delegate, exitSpan).completed("ok");

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));
        verify(delegate).completed("ok");

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
    }

    @Test
    public void failedKeepsCallerSpanActive() {
        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);
        AsyncExitSpan exitSpan = createStartedExitSpan();
        Exception cause = new RuntimeException("boom");

        new FutureCallbackWrapper<>(delegate, exitSpan).failed(cause);

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));
        verify(delegate).failed(cause);

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
    }

    @Test
    public void cancelledKeepsCallerSpanActive() {
        AbstractSpan callerSpan = ContextManager.createEntrySpan("/business", null);
        AsyncExitSpan exitSpan = createStartedExitSpan();

        new FutureCallbackWrapper<>(delegate, exitSpan).cancelled();

        assertThat(ContextManager.isActive(), is(true));
        assertThat(ContextManager.activeSpan() == callerSpan, is(true));
        verify(delegate).cancelled();

        ContextManager.stopSpan(callerSpan);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
    }
}