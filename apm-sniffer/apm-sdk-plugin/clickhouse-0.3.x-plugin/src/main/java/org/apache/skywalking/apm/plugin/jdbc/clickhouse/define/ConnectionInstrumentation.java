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

package org.apache.skywalking.apm.plugin.jdbc.clickhouse.define;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.jdbc.define.Constants;

/**
 * Intercept {@link ru.yandex.clickhouse.ClickHouseConnectionImpl} class.
 */
public class ConnectionInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private final static String ENHANCE_CLASS = "ru.yandex.clickhouse.ClickHouseConnectionImpl";
    private final static String INIT_CONNECTION_METHOD_NAME = "initConnection";
    private final static String INIT_CONNECTION_METHOD_INTERCEPTOR = "org.apache.skywalking.apm.plugin.jdbc.clickhouse.InitConnectionMethodInterceptor";
    private final static String CREATE_CLICKHOUSE_STATEMENT_METHOD_NAME = "createClickHouseStatement";
    private final static String CREATE_CLICKHOUSE_STATEMENT_INTERCEPTOR = "org.apache.skywalking.apm.plugin.jdbc.clickhouse.ClickHouseStatementMethodInterceptor";

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
        return new InstanceMethodsInterceptPoint[] {
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Constants.CREATE_STATEMENT_METHOD_NAME).or(
                                named(CREATE_CLICKHOUSE_STATEMENT_METHOD_NAME));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return CREATE_CLICKHOUSE_STATEMENT_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Constants.PREPARE_STATEMENT_METHOD_NAME);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Constants.PREPARE_STATEMENT_INTERCEPT_CLASS;
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
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(INIT_CONNECTION_METHOD_NAME).and(ElementMatchers.takesArgument(0,
                                named("ru.yandex.clickhouse.settings.ClickHouseProperties")));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return INIT_CONNECTION_METHOD_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }
}
