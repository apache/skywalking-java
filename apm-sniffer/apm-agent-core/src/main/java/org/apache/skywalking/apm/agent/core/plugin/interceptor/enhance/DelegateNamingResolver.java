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

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.RandomString;

import java.util.Objects;

/**
 * Design to generate fixed delegate field name for MethodDelegation on retransform class
 */
public class DelegateNamingResolver {
    private static final String PREFIX = "delegate$";
    private static String NAME_TRAIT = "sw";
    private final String className;
    private final int identifier;
    private final String fieldNamePrefix;

    public static void setNameTrait(String nameTrait) {
        DelegateNamingResolver.NAME_TRAIT = nameTrait;
    }

    public DelegateNamingResolver(String className, int identifier) {
        this.className = className;
        this.identifier = identifier;
        this.fieldNamePrefix = NAME_TRAIT + "_" + PREFIX + RandomString.hashOf(className.hashCode()) + "$" + RandomString.hashOf(identifier) + "$";
    }

    public String resolve(ElementMatcher<MethodDescription> matcher) {
        Objects.requireNonNull(matcher, "matcher cannot be null");
        return fieldNamePrefix + RandomString.hashOf(matcher.toString().hashCode());
    }

    public static DelegateNamingResolver get(String className, int identifier) {
        return new DelegateNamingResolver(className, identifier);
    }

}
