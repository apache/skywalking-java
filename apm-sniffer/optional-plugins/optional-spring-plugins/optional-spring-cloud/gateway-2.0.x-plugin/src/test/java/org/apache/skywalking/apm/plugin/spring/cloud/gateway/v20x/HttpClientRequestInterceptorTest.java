/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v20x;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.spring.cloud.gateway.v20x.define.EnhanceCacheObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import reactor.ipc.netty.http.client.HttpClientResponse;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(TracingSegmentRunner.class)
public class HttpClientRequestInterceptorTest {

    private HttpClientRequestInterceptor httpClientRequestInterceptor = new HttpClientRequestInterceptor();

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    private HttpClientResponse httpClientResponse;

    @Before
    public void setUp() throws Exception {

        httpClientResponse = Mockito.mock(HttpClientResponse.class);
        HttpResponseStatus httpResponseStatus = Mockito.mock(HttpResponseStatus.class);

        Mockito.when(httpResponseStatus.code()).thenReturn(200);
        Mockito.when(httpClientResponse.status()).thenReturn(httpResponseStatus);
    }

    @Test
    public void testDoAfterSuccessOrError() {
        AbstractSpan filterSpan = ContextManager.createLocalSpan("mockFilterSpan");
        filterSpan.prepareForAsync();
        ContextManager.stopSpan(filterSpan);

        AbstractSpan sendSpan = ContextManager.createExitSpan("SpringCloudGateway/sendRequest", "http://127.0.0.1:80");
        sendSpan.prepareForAsync();
        ContextManager.stopSpan(sendSpan);

        EnhanceCacheObject enhanceCacheObject = new EnhanceCacheObject(filterSpan, sendSpan);
        enhanceCacheObject = spy(enhanceCacheObject);

        //Test the ContextManager is inactive.
        httpClientRequestInterceptor.doAfterSuccessOrError(httpClientResponse, null, null);
        verify(enhanceCacheObject, Mockito.times(0)).setSpanFinish(true);

        //Test normal scenario.
        httpClientRequestInterceptor.doAfterSuccessOrError(httpClientResponse, null, enhanceCacheObject);
        verify(enhanceCacheObject, Mockito.times(1)).setSpanFinish(true);

        //Test the doAfterSuccessOrError method is executed multiple times.
        httpClientRequestInterceptor.doAfterSuccessOrError(httpClientResponse, null, enhanceCacheObject);
        verify(enhanceCacheObject, Mockito.times(1)).setSpanFinish(true);
    }

}