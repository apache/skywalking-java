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

package org.apache.skywalking.apm.plugin.jetty.v12.server.define;

import java.util.Collections;
import java.util.List;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.WitnessMethod;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * Enhances {@code org.eclipse.jetty.server.Server#handle(Request, Response, Callback)} by
 * {@code HandleInterceptor}. Only Jetty 12+ declares this three-argument, {@code Callback}-terminated
 * signature, so the witness method both activates the plugin exclusively on Jetty 12 and keeps it from
 * overlapping the merged jetty-server plugin (which enhances the Jetty 9/10/11 {@code HttpChannel},
 * removed in Jetty 12).
 */
public class Jetty12ServerInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "org.eclipse.jetty.server.Server";
    private static final String ENHANCE_METHOD = "handle";
    private static final String CALLBACK_CLASS = "org.eclipse.jetty.util.Callback";
    private static final String INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.jetty.v12.server.HandleInterceptor";

    @Override
    protected List<WitnessMethod> witnessMethods() {
        return Collections.singletonList(new WitnessMethod(
            ENHANCE_CLASS,
            named(ENHANCE_METHOD).and(takesArgument(2, named(CALLBACK_CLASS)))
        ));
    }

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
                    return named(ENHANCE_METHOD).and(takesArguments(3)).and(takesArgument(2, named(CALLBACK_CLASS)));
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return true;
                }
            }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }
}
