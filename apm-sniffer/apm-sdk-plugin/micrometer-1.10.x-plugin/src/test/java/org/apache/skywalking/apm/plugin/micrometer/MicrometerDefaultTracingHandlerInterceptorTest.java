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

package org.apache.skywalking.apm.plugin.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextManagerExtendService;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class MicrometerDefaultTracingHandlerInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @Mock
    private EnhancedInstance enhancedInstance;

    @Mock
    private MethodInterceptResult result;

    private MicrometerDefaultTracingHandlerInterceptor micrometerDefaultTracingHandlerInterceptor;

    private final Observation.Context context = new Observation.Context();

    private final Object[] consumerArguments = new Object[] {context};

    private final Class[] argumentTypes = new Class[] {Observation.Context.class};

    private final Method onStart = ObservationHandler.class.getMethod("onStart", Observation.Context.class);

    private final Method onStop = ObservationHandler.class.getMethod("onStop", Observation.Context.class);

    private final Method onError = ObservationHandler.class.getMethod("onError", Observation.Context.class);

    public MicrometerDefaultTracingHandlerInterceptorTest() throws NoSuchMethodException {
    }

    @Before
    public void setUp() throws Exception {
        micrometerDefaultTracingHandlerInterceptor = new MicrometerDefaultTracingHandlerInterceptor();

        Config.Agent.SERVICE_NAME = "MicrometerTestCases-APP";
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
    public void testDefaultStartStop() throws Throwable {
        micrometerDefaultTracingHandlerInterceptor.beforeMethod(
            enhancedInstance, onStart, consumerArguments, argumentTypes, result);
        micrometerDefaultTracingHandlerInterceptor.beforeMethod(
            enhancedInstance, onStop, consumerArguments, argumentTypes, result);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
    }

    @Test
    public void testDefailtStartExceptionStop() throws Throwable {
        micrometerDefaultTracingHandlerInterceptor.beforeMethod(
            enhancedInstance, onStart, consumerArguments, argumentTypes, result);
        context.setError(new RuntimeException("BOOM"));
        micrometerDefaultTracingHandlerInterceptor.beforeMethod(
            enhancedInstance, onError, consumerArguments, argumentTypes, result);
        micrometerDefaultTracingHandlerInterceptor.beforeMethod(
            enhancedInstance, onStop, consumerArguments, argumentTypes, result);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
    }
}
