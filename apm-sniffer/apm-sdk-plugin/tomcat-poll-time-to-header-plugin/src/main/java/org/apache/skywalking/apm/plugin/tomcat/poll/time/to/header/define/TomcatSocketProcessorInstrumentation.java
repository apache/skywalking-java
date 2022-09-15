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

package org.apache.skywalking.apm.plugin.tomcat.poll.time.to.header.define;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

public class TomcatSocketProcessorInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String TOMCAT_SOCKET_PROCESSOR_RUN = "run";
    private static final String TOMCAT_SOCKET_PROCESSOR_RESET = "reset";
    private static final String TOMCAT_SOCKET_PROCESSOR_BASE_CLASS = "org.apache.tomcat.util.net.SocketProcessorBase";
    private static final String TOMCAT_SOCKET_PROCESSOR_BASE_RUN_INTERCEPTOR = "org.apache.skywalking.apm.plugin.tomcat.poll.time.to.header.TomcatSocketProcessorBaseRunInterceptor";
    private static final String TOMCAT_SOCKET_PROCESSOR_BASE_RESET_INTERCEPTOR = "org.apache.skywalking.apm.plugin.tomcat.poll.time.to.header.TomcatSocketProcessorBaseResetInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(TOMCAT_SOCKET_PROCESSOR_BASE_CLASS);
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
                        return named(TOMCAT_SOCKET_PROCESSOR_RESET);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return TOMCAT_SOCKET_PROCESSOR_BASE_RESET_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(TOMCAT_SOCKET_PROCESSOR_RUN);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return TOMCAT_SOCKET_PROCESSOR_BASE_RUN_INTERCEPTOR;
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
}
