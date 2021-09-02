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

package org.apache.skywalking.apm.plugin.lettuce.v5.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch.byHierarchyMatch;

public class RedisCommandInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "io.lettuce.core.protocol.RedisCommand";

    private static final String REDIS_COMMAND_COMPLETE_METHOD_INTERCEPTOR = "org.apache.skywalking.apm.plugin.lettuce.v5.RedisCommandCompleteMethodInterceptor";
    private static final String REDIS_COMMAND_COMPLETE_EXCEPTIONALLY_METHOD_INTERCEPTOR = "org.apache.skywalking.apm.plugin.lettuce.v5.RedisCommandCompleteExceptionallyMethodInterceptor";
    public static final String REDIS_COMMAND_CANCEL_METHOD_INTERCEPTOR = "org.apache.skywalking.apm.plugin.lettuce.v5.RedisCommandCancelMethodInterceptor";

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
                    return named("complete").and(takesArguments(0));
                }

                @Override
                public String getMethodsInterceptor() {
                    return REDIS_COMMAND_COMPLETE_METHOD_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return true;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("completeExceptionally").and(takesArguments(1)).and(takesArgument(0, Throwable.class));
                }

                @Override
                public String getMethodsInterceptor() {
                    return REDIS_COMMAND_COMPLETE_EXCEPTIONALLY_METHOD_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("cancel").and(takesArguments(0));
                }

                @Override
                public String getMethodsInterceptor() {
                    return REDIS_COMMAND_CANCEL_METHOD_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    public ClassMatch enhanceClass() {
        return byHierarchyMatch(ENHANCE_CLASS);
    }
}
