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

package org.apache.skywalking.apm.agent.bytebuddy;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.util.Arrays;

/**
 * The actual byte-buddy's interceptor to intercept constructor methods. In this class, it provides a bridge between
 * byte-buddy and sky-walking plugin.
 */
public class ConstructorInter {
    private String interceptorClassName;
    private ClassLoader classLoader;

    /**
     * @param interceptorClassName class full name.
     */
    public ConstructorInter(String interceptorClassName, ClassLoader classLoader) {
        this.interceptorClassName = interceptorClassName;
        this.classLoader = classLoader;
    }

    /**
     * Intercept the target constructor.
     *
     * @param obj          target class instance.
     * @param allArguments all constructor arguments
     */
    @RuntimeType
    public void intercept(@This Object obj, @AllArguments Object[] allArguments) {
        EnhanceHelper.addInterceptor(interceptorClassName);
        Log.info(String.format("ConstructorInterceptorClass: %s, target: %s, args: %s", interceptorClassName, obj, Arrays.asList(allArguments)));
    }
}