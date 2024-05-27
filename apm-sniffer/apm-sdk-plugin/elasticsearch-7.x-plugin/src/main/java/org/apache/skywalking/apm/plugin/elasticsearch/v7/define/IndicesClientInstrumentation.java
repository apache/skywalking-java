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

package org.apache.skywalking.apm.plugin.elasticsearch.v7.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.elasticsearch.v7.Constants;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class IndicesClientInstrumentation extends ClassEnhancePluginDefine {

    public static final String ENHANCE_CLASS = "org.elasticsearch.client.IndicesClient";

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
                    return named("create").or(named("createAsync"))
                            .and(takesArgument(0, named(Constants.CREATE_INDEX_REQUEST_WITNESS_CLASS)));
                }

                @Override
                public String getMethodsInterceptor() {
                    return Constants.INDICES_CLIENT_CREATE_METHODS_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return true;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("delete").or(named("deleteAsync"))
                            .and(takesArgument(0, named(Constants.DELETE_INDEX_REQUEST_WITNESS_CLASS)));
                }

                @Override
                public String getMethodsInterceptor() {
                    return Constants.INDICES_CLIENT_DELETE_METHODS_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return true;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("analyze").or(named("analyzeAsync"))
                            .and(takesArgument(0, named(Constants.ANALYZE_REQUEST_WITNESS_CLASS)));
                }

                @Override
                public String getMethodsInterceptor() {
                    return Constants.INDICES_CLIENT_ANALYZE_METHODS_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return true;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("refresh").or(named("refreshAsync"))
                            .and(takesArgument(0, named(Constants.REFRESH_REQUEST_WITNESS_CLASS)));
                }

                @Override
                public String getMethodsInterceptor() {
                    return Constants.INDICES_CLIENT_REFRESH_METHODS_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return true;
                }
            }
        };
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[0];
    }

    @Override
    protected String[] witnessClasses() {
        return new String[] {
                Constants.ANALYZE_REQUEST_WITNESS_CLASS,
                Constants.CREATE_INDEX_REQUEST_WITNESS_CLASS,
                Constants.DELETE_INDEX_REQUEST_WITNESS_CLASS,
                Constants.REFRESH_REQUEST_WITNESS_CLASS
        };
    }
}
