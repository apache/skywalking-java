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

package org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance;

import net.bytebuddy.utility.RandomString;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.InstanceMethodsInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.StaticMethodsInterceptV2Point;

import java.util.Objects;

/**
 * Generate fixed delegate field name for MethodDelegation
 */
public class DelegateNamingResolver {
    private static final String PREFIX = "delegate$";
    private static String NAME_TRAIT = "sw$";
    private final String className;
    private final int identifier;
    private final String fieldNamePrefix;

    public static void setNameTrait(String nameTrait) {
        DelegateNamingResolver.NAME_TRAIT = nameTrait;
    }

    public DelegateNamingResolver(String className, int identifier) {
        this.className = className;
        this.identifier = identifier;
        // Interceptor delegate field name pattern: <name_trait>$delegate$<class_name_hash>$<plugin_define_hash>$<intercept_point_hash>
        // something like: InstMethodsInter sw$delegate$td03673$sib0lj0$5n874b1;
        this.fieldNamePrefix = NAME_TRAIT + PREFIX + RandomString.hashOf(className.hashCode()) + "$" + RandomString.hashOf(identifier) + "$";
    }

    public String resolve(Object interceptPoint) {
        Objects.requireNonNull(interceptPoint, "interceptPoint cannot be null");
        return fieldNamePrefix + RandomString.hashOf(computeHashCode(interceptPoint));
    }

    private int computeHashCode(Object interceptPoint) {
        String interceptPointClassName = interceptPoint.getClass().getName();
        if (interceptPoint instanceof ConstructorInterceptPoint) {
            ConstructorInterceptPoint point = (ConstructorInterceptPoint) interceptPoint;
            return Objects.hash(interceptPointClassName, point.getConstructorMatcher().toString(), point.getConstructorInterceptor());
        } else if (interceptPoint instanceof InstanceMethodsInterceptPoint) {
            InstanceMethodsInterceptPoint point = (InstanceMethodsInterceptPoint) interceptPoint;
            return Objects.hash(interceptPointClassName, point.getMethodsMatcher().toString(), point.getMethodsInterceptor(), point.isOverrideArgs());
        } else if (interceptPoint instanceof InstanceMethodsInterceptV2Point) {
            InstanceMethodsInterceptV2Point point = (InstanceMethodsInterceptV2Point) interceptPoint;
            return Objects.hash(interceptPointClassName, point.getMethodsMatcher().toString(), point.getMethodsInterceptorV2(), point.isOverrideArgs());
        } else if (interceptPoint instanceof StaticMethodsInterceptPoint) {
            StaticMethodsInterceptPoint point = (StaticMethodsInterceptPoint) interceptPoint;
            return Objects.hash(interceptPointClassName, point.getMethodsMatcher().toString(), point.getMethodsInterceptor(), point.isOverrideArgs());
        } else if (interceptPoint instanceof StaticMethodsInterceptV2Point) {
            StaticMethodsInterceptV2Point point = (StaticMethodsInterceptV2Point) interceptPoint;
            return Objects.hash(interceptPointClassName, point.getMethodsMatcher().toString(), point.getMethodsInterceptorV2(), point.isOverrideArgs());
        }
        return interceptPoint.hashCode();
    }

    public static DelegateNamingResolver get(String className, AbstractClassEnhancePluginDefine pluginDefine) {
        return new DelegateNamingResolver(className, pluginDefine.hashCode());
    }

}
