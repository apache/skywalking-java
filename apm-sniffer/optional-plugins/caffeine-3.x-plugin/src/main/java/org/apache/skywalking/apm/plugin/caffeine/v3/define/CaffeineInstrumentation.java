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

package org.apache.skywalking.apm.plugin.caffeine.v3.define;

import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch.byMultiClassMatch;

public class CaffeineInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    public static final String BOUNDED_LOCAL_INTERCEPT_CLASS = "com.github.benmanes.caffeine.cache.BoundedLocalCache";
    public static final String UNBOUNDED_LOCAL_INTERCEPT_CLASS = "com.github.benmanes.caffeine.cache.UnboundedLocalCache";
    public static final String CAFFEINE_ITERABLE_ENHANCE_CLASS = "org.apache.skywalking.apm.plugin.caffeine.v3.CaffeineIterableInterceptor";
    public static final String CAFFEINE_MAP_ENHANCE_CLASS = "org.apache.skywalking.apm.plugin.caffeine.v3.CaffeineMapInterceptor";
    public static final String CAFFEINE_STRING_ENHANCE_CLASS = "org.apache.skywalking.apm.plugin.caffeine.v3.CaffeineStringInterceptor";

    // read/write operations
    public static final String GET_IF_PRESENT_METHOD = "getIfPresent";
    public static final String GET_ALL_PRESENT_METHOD = "getAllPresent";
    public static final String COMPUTE_IF_ABSENT_METHOD = "computeIfAbsent";
    public static final String PUT_METHOD = "put";
    public static final String PUT_ALL_METHOD = "putAll";
    public static final String REMOVE_METHOD = "remove";
    public static final String CLEAR_METHOD = "clear";

    @Override
    protected ClassMatch enhanceClass() {
        return byMultiClassMatch(BOUNDED_LOCAL_INTERCEPT_CLASS, UNBOUNDED_LOCAL_INTERCEPT_CLASS);
    }

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
                    return named(GET_IF_PRESENT_METHOD)
                        .and(takesArguments(2))
                        .or(named(COMPUTE_IF_ABSENT_METHOD).and(takesArguments(4)))
                        .or(named(PUT_METHOD).and(takesArguments(2)))
                        .or(named(REMOVE_METHOD).and(takesArguments(1)))
                        .or(named(CLEAR_METHOD).and(takesArguments(0)));
                }

                @Override
                public String getMethodsInterceptor() {
                    return CAFFEINE_STRING_ENHANCE_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(GET_ALL_PRESENT_METHOD).and(takesArgument(0, Iterable.class));
                }

                @Override
                public String getMethodsInterceptor() {
                    return CAFFEINE_ITERABLE_ENHANCE_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(PUT_ALL_METHOD).and(takesArgument(0, Map.class));
                }

                @Override
                public String getMethodsInterceptor() {
                    return CAFFEINE_MAP_ENHANCE_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
