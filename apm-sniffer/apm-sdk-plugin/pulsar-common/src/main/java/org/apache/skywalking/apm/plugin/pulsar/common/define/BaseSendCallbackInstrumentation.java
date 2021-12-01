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

package org.apache.skywalking.apm.plugin.pulsar.common.define;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch.byHierarchyMatch;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.pulsar.common.PulsarProducerInterceptor;
import org.apache.skywalking.apm.plugin.pulsar.common.SendCallbackInterceptor;

/**
 * Pulsar producer send callback instrumentation.
 * <p>
 * The send callback enhanced object will use {@link org.apache.skywalking.apm.plugin.pulsar.common.SendCallbackEnhanceRequiredInfo}
 * which {@link PulsarProducerInterceptor} set by skywalking dynamic field of
 * enhanced object.
 * <p>
 * When a callback is complete, {@link SendCallbackInterceptor} will continue
 * the {@link org.apache.skywalking.apm.plugin.pulsar.common.SendCallbackEnhanceRequiredInfo#getContextSnapshot()}.
 */
public class BaseSendCallbackInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    public static final String ENHANCE_CLASS = "org.apache.pulsar.client.impl.SendCallback";
    public static final String ENHANCE_METHOD = "sendComplete";
    public static final String INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.pulsar.common.SendCallbackInterceptor";

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
                        return named(ENHANCE_METHOD);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return INTERCEPTOR_CLASS;
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
        return byHierarchyMatch(new String[] {ENHANCE_CLASS});
    }
}
