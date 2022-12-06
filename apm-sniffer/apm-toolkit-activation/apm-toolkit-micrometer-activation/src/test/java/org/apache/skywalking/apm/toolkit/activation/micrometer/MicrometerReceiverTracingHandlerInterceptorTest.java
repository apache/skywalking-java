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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.transport.ReceiverContext;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextManagerExtendService;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.KeyValuePair;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(TracingSegmentRunner.class)
public class MicrometerReceiverTracingHandlerInterceptorTest {

    private final ReceiverContext<Map<String, String>> context = new ReceiverContext<>(
        (carrier, key) -> carrier.get(key));
    private final Object[] consumerArguments = new Object[] {context};
    private final Class[] argumentTypes = new Class[] {ReceiverContext.class};
    private final Method onStart = ObservationHandler.class.getMethod("onStart", Observation.Context.class);
    private final Method onStop = ObservationHandler.class.getMethod("onStop", Observation.Context.class);
    private final Method onError = ObservationHandler.class.getMethod("onError", Observation.Context.class);
    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    @Mock
    private EnhancedInstance enhancedInstance;
    @Mock
    private MethodInterceptResult result;
    private MicrometerReceiverTracingHandlerInterceptor interceptor;

    public MicrometerReceiverTracingHandlerInterceptorTest() throws NoSuchMethodException {
    }

    @Before
    public void setUp() throws Exception {
        context.setName("name");
        context.setContextualName("contextualName");
        Map<String, String> map = new HashMap<>();
        map.put(
            "sw8",
            "1-Njc3OTM1ZDM4ZTFiNDAwMzk3NDU4MjI2OTM4YWU3MmIuMS4xNjY5NjM5MDI1ODE1MDAwMQ==-Njc3OTM1ZDM4ZTFiNDAwMzk3NDU4MjI2OTM4YWU3MmIuMS4xNjY5NjM5MDI1ODE1MDAwMA==-0-TWljcm9tZXRlclJlY2VpdmVyVGVzdENhc2VzLUFQUA==-YWU5YWZkM2YxMDg0NGYzMzg1MGZlOGQwYzRkNzYwYTdAMTAuMTUuMTguMTE1-Y29udGV4dHVhbE5hbWU=-aHR0cDovL2xvY2FsaG9zdDo4MDgw"
        );
        map.put("sw8-correlation", "");
        map.put("sw8-x", "0- ");
        context.setCarrier(map);

        interceptor = new MicrometerReceiverTracingHandlerInterceptor();

        Config.Agent.SERVICE_NAME = "MicrometerReceiverTestCases-APP";
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
    public void testDefaultStartExceptionStop() throws Throwable {
        interceptor.beforeMethod(
            enhancedInstance, onStart, consumerArguments, argumentTypes, result);
        context.setError(new RuntimeException("BOOM"));
        interceptor.beforeMethod(
            enhancedInstance, onError, consumerArguments, argumentTypes, result);
        interceptor.beforeMethod(
            enhancedInstance, onStop, consumerArguments, argumentTypes, result);

        AbstractTracingSpan abstractTracingSpan = onlySpan();
        assertThat(abstractTracingSpan.getOperationName(), equalTo("contextualName"));
        assertThat(abstractTracingSpan.isEntry(), is(true));
        List<LogDataEntity> logs = AbstractTracingSpanHelper.get2LevelParentFieldLogs(abstractTracingSpan);
        assertThat(logs.size(), is(1));
        assertThat(logs.get(0).getLogs().size(), is(4)); // 4 events represent an exception
        KeyValuePair errorLog = logs.get(0).getLogs().get(0);
        assertThat(errorLog.getKey(), equalTo("event"));
        assertThat(errorLog.getValue(), equalTo("error"));
    }

    private AbstractTracingSpan onlySpan() {
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        AbstractTracingSpan abstractTracingSpan = spans.get(0);
        return abstractTracingSpan;
    }
}
