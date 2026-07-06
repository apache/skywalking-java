/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package org.apache.skywalking.apm.plugin.jetty.server.define;

import org.apache.skywalking.apm.agent.core.plugin.WitnessMethod;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;

import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public abstract class AbstractWitnessInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    @Override
    protected List<WitnessMethod> witnessMethods() {
        // Match the Jetty Handler#handle whose request argument is either the javax (Jetty 9/10) or the
        // jakarta (Jetty 11) servlet namespace, so a single merged plugin activates on both. A single
        // Jetty distribution carries only one namespace, so exactly one branch matches at runtime.
        return Collections.singletonList(new WitnessMethod(
                "org.eclipse.jetty.server.Handler",
                named("handle").and(takesArgument(2, named("javax.servlet.http.HttpServletRequest")
                        .or(named("jakarta.servlet.http.HttpServletRequest"))))
        ));
    }
}
