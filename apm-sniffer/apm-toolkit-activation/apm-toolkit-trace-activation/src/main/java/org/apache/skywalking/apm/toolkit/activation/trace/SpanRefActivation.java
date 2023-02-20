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

package org.apache.skywalking.apm.toolkit.activation.trace;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;

public class SpanRefActivation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "org.apache.skywalking.apm.toolkit.trace.SpanRef";

    private static final String PREPARE_FOR_ASYNC_METHOD_NAME = "prepareForAsync";

    private static final String PREPARE_FOR_ASYNC_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.SpanRefPrepareForAsyncInterceptor";

    private static final String ASYNC_FINISH_METHOD_NAME = "asyncFinish";

    private static final String ASYNC_FINISH_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.SpanRefAsyncFinishInterceptor";

    private static final String LOG_METHOD_NAME = "log";

    private static final String LOG_THROWABLE_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.SpanRefLogThrowableInterceptor";

    private static final String LOG_MAP_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.SpanRefLogMapInterceptor";

    private static final String TAG_METHOD_NAME = "tag";

    private static final String TAG_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.SpanRefTagInterceptor";

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
                    return named(PREPARE_FOR_ASYNC_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return PREPARE_FOR_ASYNC_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ASYNC_FINISH_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return ASYNC_FINISH_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(LOG_METHOD_NAME).and(takesArgumentWithType(0, "java.lang.Throwable"));
                }

                @Override
                public String getMethodsInterceptor() {
                    return LOG_THROWABLE_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(LOG_METHOD_NAME).and(takesArgumentWithType(0, "java.util.Map"));
                }

                @Override
                public String getMethodsInterceptor() {
                    return LOG_MAP_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(TAG_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return TAG_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }
}
