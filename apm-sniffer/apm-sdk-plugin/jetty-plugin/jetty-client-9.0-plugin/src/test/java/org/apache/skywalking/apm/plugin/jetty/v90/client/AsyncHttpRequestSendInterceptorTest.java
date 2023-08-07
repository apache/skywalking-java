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

package org.apache.skywalking.apm.plugin.jetty.v90.client;

import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
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
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.ResponseNotifier;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.apache.skywalking.apm.agent.test.tools.SpanAssert.assertComponent;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(TracingSegmentRunner.class)
public class AsyncHttpRequestSendInterceptorTest {
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private MockHttpRequest httpRequestEnhancedInstance;
    private AsyncHttpRequestSendInterceptor asyncHttpRequestSendInterceptor;

    private MockResponseNotifier responseNotifierEnhancedInstance;
    private ResponseNotifierInterceptor responseNotifierInterceptor;

    @Mock
    private HttpClient httpClient;
    @Mock
    private Response response;
    private Object[] allArguments;
    private Class[] argumentTypes;
    private URI uri = URI.create("http://localhost:8080/test");

    @Before
    public void setUp() throws Exception {
        httpRequestEnhancedInstance = new MockHttpRequest(httpClient, uri);
        responseNotifierEnhancedInstance = new MockResponseNotifier(httpClient);

        Result results = new Result(httpRequestEnhancedInstance, response);
        allArguments = new Object[]{(Response.CompleteListener) result -> { }, results};
        argumentTypes = new Class[]{List.class, Result.class};

        asyncHttpRequestSendInterceptor = new AsyncHttpRequestSendInterceptor();
        responseNotifierInterceptor = new ResponseNotifierInterceptor();
    }

    @Test
    public void testHttpMethodsAround() throws Throwable {
        asyncHttpRequestSendInterceptor.beforeMethod(httpRequestEnhancedInstance, null, allArguments, argumentTypes, null);
        asyncHttpRequestSendInterceptor.afterMethod(httpRequestEnhancedInstance, null, allArguments, argumentTypes, null);
        responseNotifierInterceptor.beforeMethod(responseNotifierEnhancedInstance, null, allArguments, argumentTypes, null);
        responseNotifierInterceptor.afterMethod(responseNotifierEnhancedInstance, null, allArguments, argumentTypes, null);

        Map<String, Object> attributes = httpRequestEnhancedInstance.getAttributes();
        AbstractSpan asyncSpan = (AbstractSpan) attributes.get(Constants.SW_JETTY_EXIT_SPAN_KEY);
        assertNotNull(asyncSpan);

        assertJettySpan();

        Assert.assertEquals(false, SpanHelper.getErrorOccurred(asyncSpan));
    }

    @Test
    public void testMethodsAroundError() throws Throwable {
        asyncHttpRequestSendInterceptor.beforeMethod(httpRequestEnhancedInstance, null, allArguments, argumentTypes, null);
        asyncHttpRequestSendInterceptor.handleMethodException(httpRequestEnhancedInstance, null, allArguments, argumentTypes, new RuntimeException());
        asyncHttpRequestSendInterceptor.afterMethod(httpRequestEnhancedInstance, null, allArguments, argumentTypes, null);
        responseNotifierInterceptor.beforeMethod(responseNotifierEnhancedInstance, null, allArguments, argumentTypes, null);
        responseNotifierInterceptor.handleMethodException(responseNotifierEnhancedInstance, null, allArguments, argumentTypes, new RuntimeException());
        responseNotifierInterceptor.afterMethod(responseNotifierEnhancedInstance, null, allArguments, argumentTypes, null);

        Map<String, Object> attributes = httpRequestEnhancedInstance.getAttributes();
        AbstractSpan asyncSpan = (AbstractSpan) attributes.get(Constants.SW_JETTY_EXIT_SPAN_KEY);
        assertNotNull(asyncSpan);

        assertJettySpan();

        Assert.assertEquals(true, SpanHelper.getErrorOccurred(asyncSpan));
        SpanAssert.assertException(SpanHelper.getLogs(asyncSpan).get(0), RuntimeException.class);
    }

    private void assertJettySpan() {
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);

        Assert.assertEquals(1, SegmentHelper.getSpans(traceSegment).size());
        AbstractTracingSpan finishedSpan = SegmentHelper.getSpans(traceSegment).get(0);
        assertNotNull(finishedSpan);
        assertComponent(finishedSpan, ComponentsDefine.JETTY_CLIENT);
        MatcherAssert.assertThat(finishedSpan.isExit(), is(true));
        SpanAssert.assertLayer(finishedSpan, SpanLayer.HTTP);

        List<TagValuePair> tags = SpanHelper.getTags(finishedSpan);
        assertThat(tags.size(), is(2));
        assertThat(tags.get(0).getValue(), is("GET"));
        assertThat(tags.get(1).getValue(), is(uri.toString()));
    }

    private class MockHttpRequest extends HttpRequest implements EnhancedInstance {
        public MockHttpRequest(HttpClient httpClient, URI uri) {
            super(httpClient, uri);
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {

        }
    }

    private class MockResponseNotifier extends ResponseNotifier implements EnhancedInstance {
        public MockResponseNotifier(HttpClient client) {
            super(client);
        }

        @Override
        public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {

        }
    }
}
