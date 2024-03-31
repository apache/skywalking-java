/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.jedis.v4.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch.byHierarchyMatch;

public class ConnectionProviderInstrumentation extends AbstractWitnessInstrumentation {

    private static final String ENHANCE_INTERFACE = "redis.clients.jedis.providers.ConnectionProvider";
    private static final String CONNECTION_PROVIDER_CONSTRUCTION_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.jedis.v4.ConnectionProviderConstructorInterceptor";
    private static final String CONNECTION_PROVIDER_GET_CONNECTION_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.jedis.v4.ConnectionProviderGetConnectionInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byHierarchyMatch(ENHANCE_INTERFACE);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
            new ConstructorInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getConstructorMatcher() {
                    return ElementMatchers.takesArgument(0, named("redis.clients.jedis.HostAndPort"))
                                          .or(ElementMatchers.takesArgument(0, hasSuperType(named("java.util.Collection"))));
                }

                @Override
                public String getConstructorInterceptor() {
                    return CONNECTION_PROVIDER_CONSTRUCTION_INTERCEPT_CLASS;
                }
            }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("getConnection");
                }

                @Override
                public String getMethodsInterceptor() {
                    return CONNECTION_PROVIDER_GET_CONNECTION_INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
