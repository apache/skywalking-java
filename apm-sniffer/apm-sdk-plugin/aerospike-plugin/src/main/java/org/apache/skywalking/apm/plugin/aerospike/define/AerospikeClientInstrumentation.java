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
 */

package org.apache.skywalking.apm.plugin.aerospike.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class AerospikeClientInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    private static final String ENHANCE_CLASS = "com.aerospike.client.AerospikeClient";
    private static final String HOST_ARG_TYPE_NAME = "com.aerospike.client.policy.ClientPolicy";
    private static final String AEROSPIKE_CLIENT_CONSTRUCTOR_INTERCEPTOR = "org.apache.skywalking.apm.plugin.aerospike.AerospikeClientConstructorInterceptor";
    private static final String AEROSPIKE_CLIENT_METHOD_INTERCEPTOR = "org.apache.skywalking.apm.plugin.aerospike.AerospikeClientMethodInterceptor";
    private static final String[] ENHANCE_METHODS = new String[] {
            "append",
            "put",
            "prepend",
            "add",
            "delete",
            "touch",
            "exists",
            "get",
            "getHeader",
            "operate",
            "scanAll",
            "scanNode",
            "scanPartitions",
            "getLargeList",
            "getLargeMap",
            "getLargeSet",
            "getLargeStack",
            "register",
            "registerUdfString",
            "removeUdf",
            "execute",
            "query",
            "queryNode",
            "queryPartitions",
            "queryAggregate",
            "queryAggregateNode",
            "info"
    };

    @Override
    public ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[] {
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArgumentWithType(0, HOST_ARG_TYPE_NAME);
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return AEROSPIKE_CLIENT_CONSTRUCTOR_INTERCEPTOR;
                    }
                }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        final InstanceMethodsInterceptPoint[] points = new InstanceMethodsInterceptPoint[ENHANCE_METHODS.length];
        for (int i = 0; i < ENHANCE_METHODS.length; i++) {
            final String method = ENHANCE_METHODS[i];
            final InstanceMethodsInterceptPoint point = new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(method);
                }

                @Override
                public String getMethodsInterceptor() {
                    return AEROSPIKE_CLIENT_METHOD_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            };
            points[i] = point;
        }
        return points;
    }
}