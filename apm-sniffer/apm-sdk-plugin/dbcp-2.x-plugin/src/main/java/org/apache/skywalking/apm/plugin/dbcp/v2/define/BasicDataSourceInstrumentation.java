/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.dbcp.v2.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * BasicDataSource provides a "one stop shopping" solution for database connection pool solution
 * basic requirements. BasicDataSource#getConnection() creates (if necessary) and return a connection.
 * BasicDataSource#setUrl(String) set url
 */
public class BasicDataSourceInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    private static final String ENHANCE_CLASS = "org.apache.commons.dbcp2.BasicDataSource";
    private static final String CONNECT_GET_INTERCEPTOR = "org.apache.skywalking.apm.plugin.dbcp.v2.PoolingGetConnectInterceptor";
    private static final String INTERCEPTOR_URL_CLASS = "org.apache.skywalking.apm.plugin.dbcp.v2.PoolingSetUrlInterceptor";
    private static final String INTERCEPTOR_CREATE_CLASS = "org.apache.skywalking.apm.plugin.dbcp.v2.PoolingJmxRegisterInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
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
                        return named("getConnection");
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return CONNECT_GET_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("setUrl");
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return INTERCEPTOR_URL_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("jmxRegister");
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return INTERCEPTOR_CREATE_CLASS;
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
