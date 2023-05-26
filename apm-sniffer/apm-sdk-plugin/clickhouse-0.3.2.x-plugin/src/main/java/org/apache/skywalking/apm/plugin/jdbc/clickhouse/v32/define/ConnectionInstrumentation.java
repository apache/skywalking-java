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

package org.apache.skywalking.apm.plugin.jdbc.clickhouse.v32.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.jdbc.define.Constants;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * Intercept {@link com.clickhouse.jdbc.internal.ClickHouseConnectionImpl} class.
 */
public class ConnectionInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private final static String ENHANCE_CLASS = "com.clickhouse.jdbc.internal.ClickHouseConnectionImpl";

    private final static String ENHANCE_INIT_CONNECTION_METHOD = "com.clickhouse.jdbc.internal.ClickHouseJdbcUrlParser$ConnectionInfo";
    private final static String INIT_CONNECTION_METHOD_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.jdbc.clickhouse.v32.InitConnectionConstructorInterceptor";

    private final static String STATEMENT_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.jdbc.clickhouse.v32.ClickHouseStatementInterceptor";
    private final static String PREPARE_STATEMENT_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.jdbc.clickhouse.v32.ClickHousePrepareStatementMethodInterceptor";

    @Override
    protected final String[] witnessClasses() {
        return new String[]{ENHANCE_CLASS};
    }

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArgumentWithType(0, ENHANCE_INIT_CONNECTION_METHOD);
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return INIT_CONNECTION_METHOD_INTERCEPTOR_CLASS;
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
                        return named(Constants.CREATE_STATEMENT_METHOD_NAME);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return STATEMENT_INTERCEPT_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Constants.PREPARE_STATEMENT_METHOD_NAME).and(takesArguments(4));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return PREPARE_STATEMENT_INTERCEPT_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Constants.CLOSE_METHOD_NAME);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Constants.SERVICE_METHOD_INTERCEPT_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }
}
