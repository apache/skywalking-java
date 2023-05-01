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

package org.apache.skywalking.apm.toolkit.activation.log.log4j.v2.x.async;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * Instrument to intercept MutableLogEvent. method initFrom: initialize MutableLogEvent from another event. method
 * setLoggerName: ReusableLogEventFactory#createEvent.
 */

public class MutableLogEventInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    private static final String INTERCEPT_CLASS_INIT_FROM = "org.apache.skywalking.apm.toolkit.activation.log.log4j.v2.x.async.MutableLogEventMethodInitFromInterceptor";
    private static final String INTERCEPT_CLASS_SET_LOGGER_NAME = "org.apache.skywalking.apm.toolkit.activation.log.log4j.v2.x.async.LogEventMethodInterceptor";
    private static final String ENHANCE_CLASS = "org.apache.logging.log4j.core.async.MutableLogEvent";
    private static final String ENHANCE_METHOD_INIT_FROM = "initFrom";
    private static final String ENHANCE_METHOD_SET_LOGGER_NAME = "setLoggerName";

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
                    return named(ENHANCE_METHOD_INIT_FROM);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPT_CLASS_INIT_FROM;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(INTERCEPT_CLASS_SET_LOGGER_NAME);
                }

                @Override
                public String getMethodsInterceptor() {
                    return ENHANCE_METHOD_SET_LOGGER_NAME;
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
        return byName(ENHANCE_CLASS);
    }
}

