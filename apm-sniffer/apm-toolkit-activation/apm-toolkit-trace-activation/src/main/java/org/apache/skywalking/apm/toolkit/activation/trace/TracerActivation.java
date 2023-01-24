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
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class TracerActivation extends ClassStaticMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "org.apache.skywalking.apm.toolkit.trace.Tracer";

    private static final String CREATE_ENTRY_SPAN_METHOD_NAME = "createEntrySpan";

    private static final String CREATE_ENTRY_SPAN_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerCreateEntrySpanInterceptor";

    private static final String CREATE_LOCAL_SPAN_METHOD_NAME = "createLocalSpan";

    private static final String CREATE_LOCAL_SPAN_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerCreateLocalSpanInterceptor";

    private static final String CREATE_EXIT_SPAN_METHOD_NAME = "createExitSpan";

    private static final String CREATE_EXIT_SPAN_WITH_CONTEXT_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerCreateExitSpanWithContextInterceptor";

    private static final String CREATE_EXIT_SPAN_NO_CONTEXT_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerCreateExitSpanNoContextInterceptor";

    private static final String STOP_SPAN_METHOD_NAME = "stopSpan";

    private static final String STOP_SPAN_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerStopSpanInterceptor";

    private static final String INJECT_METHOD_NAME = "inject";

    private static final String INJECT_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerInjectInterceptor";

    private static final String EXTRACT_METHOD_NAME = "extract";

    private static final String EXTRACT_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerExtractInterceptor";

    private static final String CAPTURE_METHOD_NAME = "capture";

    private static final String CAPTURE_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerCaptureInterceptor";

    private static final String CONTINUED_METHOD_NAME = "continued";

    private static final String CONTINUED_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.TracerContinuedInterceptor";

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[] {
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(CREATE_ENTRY_SPAN_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return CREATE_ENTRY_SPAN_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(CREATE_LOCAL_SPAN_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return CREATE_LOCAL_SPAN_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(CREATE_EXIT_SPAN_METHOD_NAME).and(takesArguments(3));
                }

                @Override
                public String getMethodsInterceptor() {
                    return CREATE_EXIT_SPAN_WITH_CONTEXT_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(CREATE_EXIT_SPAN_METHOD_NAME).and(takesArguments(2));
                }

                @Override
                public String getMethodsInterceptor() {
                    return CREATE_EXIT_SPAN_NO_CONTEXT_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(STOP_SPAN_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return STOP_SPAN_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(INJECT_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INJECT_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(EXTRACT_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return EXTRACT_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(CAPTURE_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return CAPTURE_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(CONTINUED_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return CONTINUED_INTERCEPTOR_CLASS;
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
