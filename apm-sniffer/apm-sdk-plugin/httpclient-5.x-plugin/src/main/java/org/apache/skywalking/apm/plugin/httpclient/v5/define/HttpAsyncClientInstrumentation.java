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

package org.apache.skywalking.apm.plugin.httpclient.v5.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class HttpAsyncClientInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS_MINIMAL_HTTP = "org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient";
    private static final String ENHANCE_CLASS_MINIMAL_H2 = "org.apache.hc.client5.http.impl.async.MinimalH2AsyncClient";
    private static final String ENHANCE_CLASS_INTERNAL_HTTP = "org.apache.hc.client5.http.impl.async.InternalHttpAsyncClient";
    private static final String ENHANCE_CLASS_INTERNAL_H2 = "org.apache.hc.client5.http.impl.async.InternalH2AsyncClient";
    private static final String METHOD_NAME = "doExecute";
    private static final String INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.httpclient.v5.HttpAsyncClientDoExecuteInterceptor";

    @Override
    public ClassMatch enhanceClass() {
        return MultiClassNameMatch.byMultiClassMatch(
                ENHANCE_CLASS_MINIMAL_HTTP,
                ENHANCE_CLASS_MINIMAL_H2,
                ENHANCE_CLASS_INTERNAL_HTTP,
                ENHANCE_CLASS_INTERNAL_H2);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(METHOD_NAME);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return INTERCEPT_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return true;
                    }
                }
        };
    }
}
