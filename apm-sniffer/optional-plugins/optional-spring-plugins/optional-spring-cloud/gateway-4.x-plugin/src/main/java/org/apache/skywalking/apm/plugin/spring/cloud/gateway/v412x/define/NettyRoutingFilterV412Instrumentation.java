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

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v412x.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class NettyRoutingFilterV412Instrumentation extends AbstractGatewayV412EnhancePluginDefine {

    private static final String INTERCEPT_CLASS_NETTY_ROUTING_FILTER = "org.springframework.cloud.gateway.filter.NettyRoutingFilter";
    private static final String NETTY_ROUTING_FILTER_INTERCEPTOR = "org.apache.skywalking.apm.plugin.spring.cloud.gateway.v412x.NettyRoutingFilterV412Interceptor";
    private static final String NETTY_ROUTING_GET_HTTP_CLIENT_INTERCEPTOR = "org.apache.skywalking.apm.plugin.spring.cloud.gateway.v412x.NettyRoutingGetHttpClientV412Interceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(INTERCEPT_CLASS_NETTY_ROUTING_FILTER);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("filter").and(
                                takesArgumentWithType(0, "org.springframework.web.server.ServerWebExchange"));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return NETTY_ROUTING_FILTER_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return true;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("getHttpClient")
                                .and(takesArgumentWithType(0, "org.springframework.cloud.gateway.route.Route"))
                                .and(takesArgumentWithType(1, "org.springframework.web.server.ServerWebExchange"))
                                .and(returns(named("reactor.netty.http.client.HttpClient")));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return NETTY_ROUTING_GET_HTTP_CLIENT_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }
}
