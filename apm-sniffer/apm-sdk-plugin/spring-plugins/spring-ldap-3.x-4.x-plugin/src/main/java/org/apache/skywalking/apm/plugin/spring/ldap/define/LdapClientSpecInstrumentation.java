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

package org.apache.skywalking.apm.plugin.spring.ldap.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.DeclaredInstanceMethodsInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.InstanceMethodsInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch.byMultiClassMatch;

public class LdapClientSpecInstrumentation extends AbstractSpringLdapInstrumentation {

    private static final String[] ENHANCE_CLASSES = {
        "org.springframework.ldap.core.DefaultLdapClient$DefaultListSpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultListBindingsSpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultAuthenticateSpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultSearchSpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultBindSpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultModifySpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultUnbindSpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultSearchSpec$ContextMapperSearchSpec",
        "org.springframework.ldap.core.DefaultLdapClient$DefaultSearchSpec$AttributeMapperSearchSpec"
    };

    private static final String OPERATION_INTERCEPTOR =
        "org.apache.skywalking.apm.plugin.spring.ldap.SpringLdapOperationInterceptor";

    private static final String PROPAGATION_INTERCEPTOR =
        "org.apache.skywalking.apm.plugin.spring.ldap.LdapClientSpecPropagationInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byMultiClassMatch(ENHANCE_CLASSES);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptV2Point[] getInstanceMethodsInterceptV2Points() {
        return new InstanceMethodsInterceptV2Point[] {
            new DeclaredInstanceMethodsInterceptV2Point() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return isPublic().and(namedOneOf("toList", "toObject", "list", "execute"));
                }

                @Override
                public String getMethodsInterceptorV2() {
                    return OPERATION_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new DeclaredInstanceMethodsInterceptV2Point() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return isPublic().and(named("map"));
                }

                @Override
                public String getMethodsInterceptorV2() {
                    return PROPAGATION_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
