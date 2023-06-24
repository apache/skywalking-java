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

package org.apache.skywalking.apm.agent.core.util;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.ConstructorInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.InstanceMethodsInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.StaticMethodsInterceptV2Point;

import java.util.Objects;

public class InterceptPointUtil {

    public static int computeHashCode(InterceptPoint interceptPoint) {
        if (interceptPoint instanceof ConstructorInterceptPoint) {
            ConstructorInterceptPoint point = (ConstructorInterceptPoint) interceptPoint;
            return Objects.hash(point.getClass().getName(), point.getConstructorMatcher().toString(),
                    point.getConstructorInterceptor());
        } else if (interceptPoint instanceof ConstructorInterceptV2Point) {
            ConstructorInterceptV2Point point = (ConstructorInterceptV2Point) interceptPoint;
            return Objects.hash(point.getClass().getName(), point.getConstructorMatcher().toString(),
                    point.getConstructorInterceptorV2());
        } else if (interceptPoint instanceof InstanceMethodsInterceptPoint) {
            InstanceMethodsInterceptPoint point = (InstanceMethodsInterceptPoint) interceptPoint;
            return Objects.hash(point.getClass().getName(), point.getMethodsMatcher().toString(),
                    point.getMethodsInterceptor(),
                    point.isOverrideArgs());
        } else if (interceptPoint instanceof InstanceMethodsInterceptV2Point) {
            InstanceMethodsInterceptV2Point point = (InstanceMethodsInterceptV2Point) interceptPoint;
            return Objects.hash(point.getClass().getName(), point.getMethodsMatcher().toString(),
                    point.getMethodsInterceptorV2(), point.isOverrideArgs());
        } else if (interceptPoint instanceof StaticMethodsInterceptPoint) {
            StaticMethodsInterceptPoint point = (StaticMethodsInterceptPoint) interceptPoint;
            return Objects.hash(point.getClass().getName(), point.getMethodsMatcher().toString(),
                    point.getMethodsInterceptor(), point.isOverrideArgs());
        } else if (interceptPoint instanceof StaticMethodsInterceptV2Point) {
            StaticMethodsInterceptV2Point point = (StaticMethodsInterceptV2Point) interceptPoint;
            return Objects.hash(point.getClass().getName(), point.getMethodsMatcher().toString(),
                    point.getMethodsInterceptorV2(), point.isOverrideArgs());
        }
        throw new UnsupportedOperationException("Unsupported compute hashcode for InterceptPoint: " + interceptPoint);
    }

}
