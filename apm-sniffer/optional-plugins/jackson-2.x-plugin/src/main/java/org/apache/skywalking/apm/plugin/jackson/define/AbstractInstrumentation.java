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

package org.apache.skywalking.apm.plugin.jackson.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Jackson provides a "one stop" solution for json serialization and deserialization solution
 * basic requirements.
 */

public abstract class AbstractInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {

        final List<InstanceMethodsInterceptPoint> points = new ArrayList<>(this.enhanceMethods().size());

        for (Map.Entry<String, String> entry : this.enhanceMethods().entrySet()) {
            final InstanceMethodsInterceptPoint point = new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(entry.getKey());
                }

                @Override
                public String getMethodsInterceptor() {
                    return entry.getValue();
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            };
            points.add(point);
        }

        return points.toArray(new InstanceMethodsInterceptPoint[0]);
    }

    /**
     * Define the method and interceptor kev/value. method name is key, interceptor class is value.
     *
     * @return enhance methods
     */
    protected abstract Map<String, String> enhanceMethods();

}
