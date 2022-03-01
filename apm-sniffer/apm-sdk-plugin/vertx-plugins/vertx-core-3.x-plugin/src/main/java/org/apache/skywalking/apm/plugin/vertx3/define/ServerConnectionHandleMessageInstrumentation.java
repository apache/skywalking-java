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

package org.apache.skywalking.apm.plugin.vertx3.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * {@link ServerConnectionHandleMessageInstrumentation} enhance the <code>handleMessage</code> method in
 * <code>io.vertx.core.http.impl.ServerConnection</code> and <code>io.vertx.core.http.impl.Http1xServerConnection</code>
 * classes by <code>ServerConnectionHandleMessageInterceptor</code> class.
 */
public class ServerConnectionHandleMessageInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String SERVER_CONNECTION_ENHANCE_CLASS = "io.vertx.core.http.impl.ServerConnection";
    private static final String HTTP_SERVER_CONNECTION_ENHANCE_CLASS = "io.vertx.core.http.impl.Http1xServerConnection";
    private static final String ENHANCE_METHOD = "handleMessage";
    private static final String INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.vertx3.ServerConnectionHandleMessageInterceptor";

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return MultiClassNameMatch.byMultiClassMatch(
                HTTP_SERVER_CONNECTION_ENHANCE_CLASS, //ver. 3.5.1+
                SERVER_CONNECTION_ENHANCE_CLASS //ver. 3.0.0 - 3.5.0
        );
    }

    @Override
    protected String[] witnessClasses() {
        return new String[] {"io.vertx.core.http.impl.WebSocketFrameFactoryImpl"};
    }
}
