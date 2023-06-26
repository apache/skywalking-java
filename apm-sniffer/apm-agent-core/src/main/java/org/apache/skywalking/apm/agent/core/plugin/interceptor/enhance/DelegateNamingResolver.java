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
import org.apache.skywalking.apm.agent.core.conf.Constants;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.ConstructorInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.InstanceMethodsInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.StaticMethodsInterceptV2Point;

import java.util.Objects;

/**
 * Generate fixed delegate field name for MethodDelegation
 */
public class DelegateNamingResolver {
    private static final String PREFIX = "delegate$";
    private final String fieldNamePrefix;

    public DelegateNamingResolver(String className, AbstractClassEnhancePluginDefine pluginDefine) {
        // Interceptor delegate field name pattern: <name_trait>$delegate$<class_name_hash>$<plugin_define_hash>$<intercept_point_hash>
        // something like: InstMethodsInter sw$delegate$td03673$sib0lj0$5n874b1;
        this.fieldNamePrefix = Constants.NAME_TRAIT + PREFIX + RandomString.hashOf(className.hashCode()) + "$" + RandomString.hashOf(pluginDefine.hashCode()) + "$";
    }

    public String resolve(ConstructorInterceptPoint interceptPoint) {
        Objects.requireNonNull(interceptPoint, "interceptPoint cannot be null");
        return fieldNamePrefix + RandomString.hashOf(interceptPoint.computeHashCode());
    }

    public String resolve(ConstructorInterceptV2Point interceptPoint) {
        Objects.requireNonNull(interceptPoint, "interceptPoint cannot be null");
        return fieldNamePrefix + RandomString.hashOf(interceptPoint.computeHashCode());
    }

    public String resolve(InstanceMethodsInterceptPoint interceptPoint) {
        Objects.requireNonNull(interceptPoint, "interceptPoint cannot be null");
        return fieldNamePrefix + RandomString.hashOf(interceptPoint.computeHashCode());
    }

    public String resolve(InstanceMethodsInterceptV2Point interceptPoint) {
        Objects.requireNonNull(interceptPoint, "interceptPoint cannot be null");
        return fieldNamePrefix + RandomString.hashOf(interceptPoint.computeHashCode());
    }

    public String resolve(StaticMethodsInterceptPoint interceptPoint) {
        Objects.requireNonNull(interceptPoint, "interceptPoint cannot be null");
        return fieldNamePrefix + RandomString.hashOf(interceptPoint.computeHashCode());
    }

    public String resolve(StaticMethodsInterceptV2Point interceptPoint) {
        Objects.requireNonNull(interceptPoint, "interceptPoint cannot be null");
        return fieldNamePrefix + RandomString.hashOf(interceptPoint.computeHashCode());
    }
}
