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

package org.apache.skywalking.apm.plugin.elasticsearch.v6.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.elasticsearch.v6.interceptor.Constants;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class TransportActionNodeProxyInstrumentation extends ClassEnhancePluginDefine {

    public static final String INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.elasticsearch.v6.interceptor.TransportActionNodeProxyExecuteMethodsInterceptor";
    public static final String THREE_ARGS_CONSTRUCTOR_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.elasticsearch.v6.interceptor.TransportActionNodeProxyThreeArgsConstructorInterceptor";
    public static final String TWO_ARGS_CONSTRUCTOR2_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.elasticsearch.v6.interceptor.TransportActionNodeProxyTwoArgsConstructorInterceptor";
    public static final String ENHANC_CLASS = "org.elasticsearch.action.TransportActionNodeProxy";
    public static final String CONSTRUCTOR_ARG_CLASS = "org.elasticsearch.transport.TransportService";

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
            new ConstructorInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getConstructorMatcher() {
                    return ElementMatchers.takesArgument(2, named(CONSTRUCTOR_ARG_CLASS));
                }

                @Override public String getConstructorInterceptor() {
                    return THREE_ARGS_CONSTRUCTOR_INTERCEPTOR_CLASS;
                }
            },
            new ConstructorInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getConstructorMatcher() {
                    return ElementMatchers.takesArgument(1, named(CONSTRUCTOR_ARG_CLASS));
                }

                @Override
                public String getConstructorInterceptor() {
                    return TWO_ARGS_CONSTRUCTOR2_INTERCEPTOR_CLASS;
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
                    return named("execute");
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[0];
    }

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANC_CLASS);
    }

    @Override
    protected String[] witnessClasses() {
        return new String[]{Constants.TASK_TRANSPORT_CHANNEL_WITNESS_CLASSES};
    }
}
