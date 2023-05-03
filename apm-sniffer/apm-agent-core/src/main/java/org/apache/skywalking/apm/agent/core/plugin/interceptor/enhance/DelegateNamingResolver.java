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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Design to generate fixed delegate field name for MethodDelegation on retransform class
 */
public class DelegateNamingResolver {
    private static final String PREFIX = "delegate$";
    private static String NAME_TRAIT = "sw";
    private static Map<String, AtomicInteger> NAME_CACHE = new ConcurrentHashMap<>();
    private final String className;
    private final String fileNamePrefix;

    public static void setNameTrait(String nameTrait) {
        DelegateNamingResolver.NAME_TRAIT = nameTrait;
    }

    public DelegateNamingResolver(String className) {
        this.className = className;
        fileNamePrefix = NAME_TRAIT + "_" + PREFIX + RandomString.hashOf(className.hashCode()) + "$";
    }

    public String next() {
        AtomicInteger index = NAME_CACHE.computeIfAbsent(className, key -> new AtomicInteger(0));
        return fileNamePrefix + index.incrementAndGet();
    }

    public static DelegateNamingResolver get(String className) {
        return new DelegateNamingResolver(className);
    }

    /**
     * do reset before retransform
     */
    public static void reset(String className) {
        NAME_CACHE.remove(className);
    }
}
