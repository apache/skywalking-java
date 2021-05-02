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

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * This class instrument {@link org.springframework.cloud.gateway.filter.NettyRoutingFilter} class.
 * <p>
 * Enhance <code>filter</code> method and get <code>ContextSnapshot</code> from {@link
 * org.springframework.web.server.ServerWebExchange}. Create a local span to continue context snapshot created in {@link
 * org.apache.skywalking.apm.plugin.spring.webflux.v5.DispatcherHandlerHandleMethodInterceptor} interceptor.
 * </p>
 */
public class NettyRoutingFilterInstrumentation extends AbstractGatewayV3EnhancePluginDefine {

    private static final String INTERCEPT_CLASS_NETTY_ROUTING_FILTER = "org.springframework.cloud.gateway.filter.NettyRoutingFilter";
    private static final String NETTY_ROUTING_FILTER_INTERCEPTOR = "org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x.NettyRoutingFilterInterceptor";

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
                }
        };
    }
}
