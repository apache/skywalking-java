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

package org.apache.skywalking.apm.toolkit.activation.webflux;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class WebFluxSkyWalkingTraceContextActivation extends ClassStaticMethodsEnhancePluginDefine {

    public static final String TRACE_ID_INTERCEPT_CLASS = "org.apache.skywalking.apm.toolkit.activation.webflux.WebFluxSkyWalkingTraceIDInterceptor";
    public static final String SEGMENT_ID_INTERCEPT_CLASS = "org.apache.skywalking.apm.toolkit.activation.webflux.WebFluxSkyWalkingSegmentIDInterceptor";
    public static final String SPAN_ID_INTERCEPT_CLASS = "org.apache.skywalking.apm.toolkit.activation.webflux.WebFluxSkyWalkingSpanIDInterceptor";
    /**
     * The un-versioned name shipped by apm-toolkit-webflux 9.7.0 and earlier. Still matched so that
     * applications which never upgrade their toolkit keep working against a newer agent.
     */
    public static final String ENHANCE_CLASS = "org.apache.skywalking.apm.toolkit.webflux.WebFluxSkyWalkingTraceContext";
    /**
     * apm-toolkit-webflux-5.x, for Reactor 3.1-3.4 (Spring Boot 2.x).
     */
    public static final String ENHANCE_CLASS_V5 = "org.apache.skywalking.apm.toolkit.webflux.v5.WebFluxSkyWalkingTraceContext";
    /**
     * apm-toolkit-webflux-6.x, for Reactor 3.5+ (Spring Boot 3.x and 4.x).
     */
    public static final String ENHANCE_CLASS_V6 = "org.apache.skywalking.apm.toolkit.webflux.v6.WebFluxSkyWalkingTraceContext";
    public static final String ENHANCE_TRACE_ID_METHOD = "traceId";
    public static final String ENHANCE_SEGMENT_ID_METHOD = "segmentId";
    public static final String ENHANCE_SPAN_ID_METHOD = "spanId";
    public static final String ENHANCE_GET_CORRELATION_METHOD = "getCorrelation";
    public static final String INTERCEPT_GET_CORRELATION_CLASS = "org.apache.skywalking.apm.toolkit.activation.webflux.WebFluxSkyWalkingCorrelationContextGetInterceptor";
    public static final String ENHANCE_PUT_CORRELATION_METHOD = "putCorrelation";
    public static final String INTERCEPT_PUT_CORRELATION_CLASS = "org.apache.skywalking.apm.toolkit.activation.webflux.WebFluxSkyWalkingCorrelationContextPutInterceptor";

    /**
     * @return the target class, which needs active.
     */
    @Override
    protected ClassMatch enhanceClass() {
        return MultiClassNameMatch.byMultiClassMatch(ENHANCE_CLASS, ENHANCE_CLASS_V5, ENHANCE_CLASS_V6);
    }

    /**
     * @return the collection of {@link StaticMethodsInterceptPoint}, represent the intercepted methods and their
     * interceptors.
     */
    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[] {
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_TRACE_ID_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return TRACE_ID_INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_SEGMENT_ID_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return SEGMENT_ID_INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_SPAN_ID_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return SPAN_ID_INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },

            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_GET_CORRELATION_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPT_GET_CORRELATION_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_PUT_CORRELATION_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPT_PUT_CORRELATION_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
