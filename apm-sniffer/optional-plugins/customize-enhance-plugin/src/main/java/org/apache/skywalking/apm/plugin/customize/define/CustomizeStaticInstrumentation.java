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

package org.apache.skywalking.apm.plugin.customize.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.ClassStaticMethodsEnhancePluginDefineV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.StaticMethodsInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;
import org.apache.skywalking.apm.plugin.customize.conf.CustomizeConfiguration;

/**
 * The static of customize instrumentation.
 */

public class CustomizeStaticInstrumentation extends ClassStaticMethodsEnhancePluginDefineV2 {
    private String enhanceClass;

    public CustomizeStaticInstrumentation(String enhanceClass) {
        this.enhanceClass = enhanceClass;
    }

    @Override
    public StaticMethodsInterceptV2Point[] getStaticMethodsInterceptV2Points() {
        final ElementMatcher matcher = CustomizeConfiguration.INSTANCE.getInterceptPoints(enhanceClass, true);
        if (matcher == null) {
            return new StaticMethodsInterceptV2Point[0];
        } else {
            return new StaticMethodsInterceptV2Point[] {
                new StaticMethodsInterceptV2Point() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return matcher;
                    }

                    @Override
                    public String getMethodsInterceptorV2() {
                        return "org.apache.skywalking.apm.plugin.customize.interceptor.CustomizeStaticInterceptor";
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
            };
        }
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(enhanceClass);
    }
}
