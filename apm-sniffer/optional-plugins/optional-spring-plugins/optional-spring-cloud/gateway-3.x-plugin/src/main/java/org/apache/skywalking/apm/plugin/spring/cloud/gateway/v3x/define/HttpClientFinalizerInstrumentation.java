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
 * This class instrument <code>reactor.netty.http.client.HttpClientFinalizer</code> class.
 * <p>
 * This is the class that actually sends the request. By enhancing in different methods,
 * we can get information such as uri and send trace context to the downstream service through http header.
 * </p>
 */
public class HttpClientFinalizerInstrumentation extends AbstractGatewayV3EnhancePluginDefine {

    private static final String INTERCEPT_CLASS_HTTP_CLIENT_FINALIZER = "reactor.netty.http.client.HttpClientFinalizer";
    private static final String CLIENT_FINALIZER_CONSTRUCTOR_INTERCEPTOR = "org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x.HttpClientFinalizerConstructorInterceptor";
    private static final String CLIENT_FINALIZER_CONSTRUCTOR_ARGUMENT_TYPE = "reactor.netty.http.client.HttpClientConfig";
    private static final String HTTP_CLIENT_FINALIZER_URI_INTERCEPTOR = "org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x.HttpClientFinalizerUriInterceptor";
    private static final String HTTP_CLIENT_FINALIZER_SEND_INTERCEPTOR = "org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x.HttpClientFinalizerSendInterceptor";
    private static final String HTTP_CLIENT_FINALIZER_RESPONSE_CONNECTION_INTERCEPTOR = "org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x.HttpClientFinalizerResponseConnectionInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(INTERCEPT_CLASS_HTTP_CLIENT_FINALIZER);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArgumentWithType(0, CLIENT_FINALIZER_CONSTRUCTOR_ARGUMENT_TYPE);
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return CLIENT_FINALIZER_CONSTRUCTOR_INTERCEPTOR;
                    }
                }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("uri");
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return HTTP_CLIENT_FINALIZER_URI_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("send").and(takesArgumentWithType(0, "java.util.function.BiFunction"));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return HTTP_CLIENT_FINALIZER_SEND_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return true;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("responseConnection");
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return HTTP_CLIENT_FINALIZER_RESPONSE_CONNECTION_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return true;
                    }
                }
        };
    }
}
