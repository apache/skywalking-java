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

package org.apache.skywalking.apm.plugin.jetty.thread.pool.define;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

import java.util.Collections;
import java.util.List;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.WitnessMethod;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

public class JettyServerInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String JETTY_SERVER_CLASS = "org.eclipse.jetty.server.Server";

    private static final String JETTY_SERVER_INTERCEPTOR = "org.apache.skywalking.apm.plugin.jetty.thread.pool.JettyServerInterceptor";

    private static final String QUEUED_THREAD_POOL_CLASS_NAME = "org.eclipse.jetty.util.thread.QueuedThreadPool";

    private static final String QUEUED_THREAD_POOL_GET_QUEUE_SIZE = "getQueueSize";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(JETTY_SERVER_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{new ConstructorInterceptPoint() {
            @Override
            public ElementMatcher<MethodDescription> getConstructorMatcher() {
                return any();
            }

            @Override
            public String getConstructorInterceptor() {
                return JETTY_SERVER_INTERCEPTOR;
            }
        }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[0];
    }

    @Override
    protected List<WitnessMethod> witnessMethods() {
        return Collections.singletonList(new WitnessMethod(
                QUEUED_THREAD_POOL_CLASS_NAME,
                named(QUEUED_THREAD_POOL_GET_QUEUE_SIZE)
        ));
    }
}
