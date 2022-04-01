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

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v21x;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class NettyRoutingFilterInterceptorTest {
    private final NettyRoutingFilterInterceptor interceptor = new NettyRoutingFilterInterceptor();
    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Before
    public void setUp() throws Exception {
    }

    private static final String NETTY_ROUTING_FILTER_TRACED_ATTR =
            NettyRoutingFilterInterceptor.class.getName() + ".isTraced";

    private final ServerWebExchange exchange = new ServerWebExchange() {
        Map<String, Object> attributes = new HashMap<>();
        @Override
        public ServerHttpRequest getRequest() {
            return null;
        }

        @Override
        public ServerHttpResponse getResponse() {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Mono<WebSession> getSession() {
            return null;
        }

        @Override
        public <T extends Principal> Mono<T> getPrincipal() {
            return null;
        }

        @Override
        public Mono<MultiValueMap<String, String>> getFormData() {
            return null;
        }

        @Override
        public Mono<MultiValueMap<String, Part>> getMultipartData() {
            return null;
        }

        @Override
        public LocaleContext getLocaleContext() {
            return null;
        }

        @Override
        public ApplicationContext getApplicationContext() {
            return null;
        }

        @Override
        public boolean isNotModified() {
            return false;
        }

        @Override
        public boolean checkNotModified(Instant instant) {
            return false;
        }

        @Override
        public boolean checkNotModified(String s) {
            return false;
        }

        @Override
        public boolean checkNotModified(String s, Instant instant) {
            return false;
        }

        @Override
        public String transformUrl(String s) {
            return null;
        }

        @Override
        public void addUrlTransformer(Function<String, String> function) {

        }

        @Override
        public String getLogPrefix() {
            return null;
        }
    };

    @Test
    public void testIsTraced() throws Throwable {
        interceptor.beforeMethod(null, null, new Object[]{exchange}, null, null);
        interceptor.afterMethod(null, null, null, null, null);
        Assert.assertEquals(exchange.getAttributes().get(NETTY_ROUTING_FILTER_TRACED_ATTR), true);
        Assert.assertNotNull(ContextManager.activeSpan());

        ContextManager.stopSpan();

        interceptor.beforeMethod(null, null, new Object[]{exchange}, null, null);
        interceptor.afterMethod(null, null, null, null, null);
        Assert.assertEquals(exchange.getAttributes().get(NETTY_ROUTING_FILTER_TRACED_ATTR), true);
    }
}
