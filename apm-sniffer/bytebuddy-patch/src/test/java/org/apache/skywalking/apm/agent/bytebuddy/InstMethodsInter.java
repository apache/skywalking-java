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
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class InstMethodsInter {
    private String interceptorClassName;
    private ClassLoader classLoader;

    public InstMethodsInter(String interceptorClassName, ClassLoader classLoader) {
        this.interceptorClassName = interceptorClassName;
        this.classLoader = classLoader;
    }

    @RuntimeType
    public Object intercept(@This Object obj, @AllArguments Object[] allArguments, @SuperCall Callable<?> zuper,
                            @Origin Method method) throws Throwable {
        EnhanceHelper.addInterceptor(interceptorClassName);

        Object originResult = zuper.call();
        Object finalResult;
        if (originResult instanceof String) {
            String result = (String) originResult;
            result = result.replaceAll("Joe", "John");
            finalResult = result;
        } else if (originResult instanceof Integer) {
            Integer result = (Integer) originResult;
            finalResult = result + 1;
        } else {
            finalResult = originResult;
        }

        Log.info(String.format("InstMethodInterceptorClass: %s, target: %s, args: %s, SuperCall: %s, method: %s, originResult: %s, finalResult: %s",
                interceptorClassName, obj, Arrays.asList(allArguments), zuper, method, originResult, finalResult));
        return finalResult;
    }

}
