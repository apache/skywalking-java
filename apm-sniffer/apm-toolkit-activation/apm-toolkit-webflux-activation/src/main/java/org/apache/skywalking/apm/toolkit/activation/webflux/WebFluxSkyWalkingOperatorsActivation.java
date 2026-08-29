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

package org.apache.skywalking.apm.toolkit.activation.webflux;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch.byMultiClassMatch;

/**
 */
public class WebFluxSkyWalkingOperatorsActivation extends ClassStaticMethodsEnhancePluginDefine {

    public static final String INTERCEPT_CLASS =
            "org.apache.skywalking.apm.toolkit.activation.webflux.WebFluxSkyWalkingOperatorsInterceptor";
    /**
     * The un-versioned name shipped by apm-toolkit-webflux 9.7.0 and earlier. Still matched so that
     * applications which never upgrade their toolkit keep working against a newer agent.
     */
    public static final String ENHANCE_CLASS =
            "org.apache.skywalking.apm.toolkit.webflux.WebFluxSkyWalkingOperators";
    /**
     * apm-toolkit-webflux-5.x, for Reactor 3.1-3.4 (Spring Boot 2.x).
     */
    public static final String ENHANCE_CLASS_V5 =
            "org.apache.skywalking.apm.toolkit.webflux.v5.WebFluxSkyWalkingOperators";
    /**
     * apm-toolkit-webflux-6.x, for Reactor 3.5+ (Spring Boot 3.x and 4.x).
     */
    public static final String ENHANCE_CLASS_V6 =
            "org.apache.skywalking.apm.toolkit.webflux.v6.WebFluxSkyWalkingOperators";
    public static final String ENHANCE_METHOD = "continueTracing";

    @Override
    protected ClassMatch enhanceClass() {
        return byMultiClassMatch(ENHANCE_CLASS, ENHANCE_CLASS_V5, ENHANCE_CLASS_V6);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[] {
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(ENHANCE_METHOD).and(takesArguments(2))
                                .and(takesArgument(0, named("reactor.util.context.Context"))
                                        .or(takesArgument(0, named("org.springframework.web.server.ServerWebExchange"))));
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
}
