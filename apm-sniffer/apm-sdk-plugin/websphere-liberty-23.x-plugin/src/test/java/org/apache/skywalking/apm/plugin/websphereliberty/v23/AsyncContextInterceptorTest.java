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

package org.apache.skywalking.apm.plugin.websphereliberty.v23;

import com.ibm.websphere.servlet.request.IRequest;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.websphereliberty.v23.async.RunnableWrapper;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.apache.skywalking.apm.agent.test.tools.SpanAssert.assertComponent;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class AsyncContextInterceptorTest {

    private AsyncContextInterceptor asyncContextInterceptor;
    private WebContainerInterceptor handleRequestInterceptor;

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private IRequest request;
    @Mock
    private EnhancedInstance response;

    @Mock
    private Runnable runnable;

    @Mock
    private MethodInterceptResult containerInterceptResult;
    @Mock
    private MethodInterceptResult asyncInterceptResult;

    @Mock
    private EnhancedInstance mockWebContainer;
    @Mock
    private EnhancedInstance mockAsyncContext;

    private Object[] containerArguments;
    private Class[] containerArgumentType;

    private Object[] asyncArguments;
    private Class[] asyncArgumentType;

    @Before
    public void setUp() throws Exception {
        asyncContextInterceptor = new AsyncContextInterceptor();
        handleRequestInterceptor = new WebContainerInterceptor();

        when(request.getMethod()).thenReturn("GET");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getRequestURI()).thenReturn("/test/testRequestURL");

        when(response.getSkyWalkingDynamicField()).thenReturn(200);

        containerArguments = new Object[] {
            request,
            response
        };
        containerArgumentType = new Class[] {
            request.getClass(),
            response.getClass()
        };

        asyncArguments = new Object[] {
            runnable
        };
        asyncArgumentType = new Class[] {
            runnable.getClass()
        };
    }

    @Test
    public void testAsyncContext() throws Throwable {
        handleRequestInterceptor.beforeMethod(mockWebContainer, null, containerArguments, containerArgumentType,
                                              containerInterceptResult
        );

        asyncContextInterceptor.beforeMethod(
            mockAsyncContext, null, asyncArguments, asyncArgumentType, asyncInterceptResult);

        Thread thread = new Thread(() -> {
            RunnableWrapper runnableWrappers = (RunnableWrapper) asyncArguments[0];
            runnableWrappers.run();
        });

        asyncContextInterceptor.afterMethod(mockAsyncContext, null, asyncArguments, asyncArgumentType, null);
        handleRequestInterceptor.afterMethod(mockWebContainer, null, containerArguments, containerArgumentType, null);

        thread.start();
        thread.join();

        assertThat(segmentStorage.getTraceSegments().size(), is(2));

        Assert.assertEquals(
            segmentStorage.getTraceSegments().get(0).getRelatedGlobalTrace(),
            segmentStorage.getTraceSegments().get(1).getRelatedGlobalTrace()
        );

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertAsyncSpan(spans.get(0));
    }

    private void assertAsyncSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), CoreMatchers.startsWith("WebSphereAsync"));
        assertComponent(span, ComponentsDefine.JDK_THREADING);
        SpanAssert.assertLayer(span, SpanLayer.HTTP);
    }
}
