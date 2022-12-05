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

package org.apache.skywalking.apm.toolkit.activation.micrometer;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextManagerExtendService;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingContextSnapshotThreadLocalAccessor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@RunWith(TracingSegmentRunner.class)
public class MicrometerContextSnapshotThreadLocalAccessorInterceptorTest {

    private final Class[] argumentTypes = new Class[] {Object.class};
    private final Method get = SkywalkingContextSnapshotThreadLocalAccessor.class.getMethod("getValue");
    private final Method set = SkywalkingContextSnapshotThreadLocalAccessor.class.getMethod("setValue", Object.class);
    private final Method reset = SkywalkingContextSnapshotThreadLocalAccessor.class.getMethod("reset");
    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    @Mock
    private EnhancedInstance enhancedInstance;
    @Mock
    private MethodInterceptResult result;
    private MicrometerContextSnapshotThreadLocalAccessorInterceptor interceptor;
    private ContextSnapshot context;
    private Object[] consumerArguments;
    private AbstractSpan testSpan;

    public MicrometerContextSnapshotThreadLocalAccessorInterceptorTest() throws NoSuchMethodException {
    }

    @Before
    public void setUp() throws Exception {
        interceptor = new MicrometerContextSnapshotThreadLocalAccessorInterceptor();

        Config.Agent.SERVICE_NAME = "MicrometerContextSnapshotThreadLocalAccessorTestCases-APP";

        testSpan = ContextManager.createLocalSpan("test from threadlocalaccessor test");
        context = ContextManager.capture();
        consumerArguments = new Object[] {context};
    }

    @After
    public void clear() {
        assertThat(ContextManager.isActive(), is(false));
    }

    @AfterClass
    public static void clearAfterAll() {
        // test from threadlocalaccessor test x 2 TODO: I have no idea what is going on
        ContextManager.stopSpan();
        ContextManager.stopSpan();
        assertThat(ContextManager.isActive(), is(false));
    }

    @Test
    public void testServiceFromPlugin() {
        PluginBootService service = ServiceManager.INSTANCE.findService(
            PluginBootService.class);

        Assert.assertNotNull(service);
    }

    @Test
    public void testServiceOverrideFromPlugin() {
        ContextManagerExtendService service = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);

        Assert.assertTrue(service instanceof ContextManagerExtendOverrideService);
    }

    @Test
    public void testGetSetReset() throws Throwable {
        ContextSnapshot contextSnapshot = (ContextSnapshot) interceptor.afterMethod(
            enhancedInstance, get, new Object[0], new Class[0], result);

        ContextSnapshot currentCapturedContext = ContextManager.capture();
        assertThat(currentCapturedContext, not(equalTo(contextSnapshot)));
        assertThat(currentCapturedContext.getTraceId(), equalTo(contextSnapshot.getTraceId()));

        ContextManager.stopSpan();

        interceptor.afterMethod(
            enhancedInstance, set, consumerArguments, argumentTypes, result);

        ContextManager.stopSpan();

        assertThat(segmentStorage.getTraceSegments().size(), is(2));
        TraceSegment parent = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(parent);
        assertThat(spans.size(), is(1));
        String operationName = spans.get(0).getOperationName();
        assertThat(operationName, is("test from threadlocalaccessor test"));

        TraceSegment continuedParent = segmentStorage.getTraceSegments().get(1);
        assertThat(continuedParent.getRef().getParentEndpoint(), is(operationName));

        interceptor.afterMethod(
            enhancedInstance, reset, new Object[0], new Class[0], result);

        assertThat(ContextManager.isActive(), is(false));
    }

}
