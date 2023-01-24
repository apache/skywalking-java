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

public class CarrierItemRefActivation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "org.apache.skywalking.apm.toolkit.trace.CarrierItemRef";

    private static final String HAS_NEXT_METHOD_NAME = "hasNext";

    private static final String HAS_NEXT_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.CarrierItemRefHasNextInterceptor";

    private static final String NEXT_METHOD_NAME = "next";

    private static final String NEXT_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.CarrierItemRefNextInterceptor";

    private static final String GET_HEADKEY_METHOD_NAME = "getHeadKey";

    private static final String GET_HEADKEY_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.CarrierItemGetHeadKeyInterceptor";

    private static final String GET_HEADVALUE_METHOD_NAME = "getHeadValue";

    private static final String GET_HEADVALUE_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.CarrierItemGetHeadValueInterceptor";

    private static final String SET_HEADVALUE_METHOD_NAME = "setHeadValue";

    private static final String SET_HEADVALUE_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.toolkit.activation.trace.CarrierItemSetHeadValueInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
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
                    return named(HAS_NEXT_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return HAS_NEXT_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(NEXT_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return NEXT_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(GET_HEADKEY_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return GET_HEADKEY_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(GET_HEADVALUE_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return GET_HEADVALUE_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(SET_HEADVALUE_METHOD_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return SET_HEADVALUE_INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
