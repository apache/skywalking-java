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

package org.apache.skywalking.apm.plugin.mongodb.v5.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

/**
 * Enhance {@code MongoClusterImpl$OperationExecutorImpl} in MongoDB driver 5.2+.
 * <p>
 * Constructor interception: propagate remotePeer from enclosing MongoClusterImpl
 * (synthetic arg[0] for non-static inner class).
 * <p>
 * Method interception: create exit spans on execute() calls.
 * Reuses the 4.x MongoDBOperationExecutorInterceptor.
 */
public class MongoClusterOperationExecutorInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS =
        "com.mongodb.client.internal.MongoClusterImpl$OperationExecutorImpl";

    private static final String CONSTRUCTOR_INTERCEPTOR =
        "org.apache.skywalking.apm.plugin.mongodb.v5.interceptor.OperationExecutorImplConstructorInterceptor";

    private static final String EXECUTE_INTERCEPTOR =
        "org.apache.skywalking.apm.plugin.mongodb.v4.interceptor.MongoDBOperationExecutorInterceptor";

    private static final String METHOD_NAME = "execute";

    private static final String ARGUMENT_TYPE = "com.mongodb.client.ClientSession";

    @Override
    protected String[] witnessClasses() {
        return new String[] {"com.mongodb.client.internal.MongoClusterImpl"};
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[] {
            new ConstructorInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getConstructorMatcher() {
                    return ElementMatchers.any();
                }

                @Override
                public String getConstructorInterceptor() {
                    return CONSTRUCTOR_INTERCEPTOR;
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
                    return ElementMatchers
                        .named(METHOD_NAME)
                        .and(ArgumentTypeNameMatch.takesArgumentWithType(2, ARGUMENT_TYPE))
                        .or(ElementMatchers.<MethodDescription>named(METHOD_NAME)
                            .and(ArgumentTypeNameMatch.takesArgumentWithType(3, ARGUMENT_TYPE)));
                }

                @Override
                public String getMethodsInterceptor() {
                    return EXECUTE_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
