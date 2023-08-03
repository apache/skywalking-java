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

package org.apache.skywalking.apm.plugin.jetty.v90.client;

import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;

import java.lang.reflect.Method;
import java.util.Optional;

public class ResponseNotifierInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        // get async span and stop it.
        Optional.ofNullable(getAsyncSpan(allArguments)).ifPresent(v -> v.asyncFinish());
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        Optional.ofNullable(getAsyncSpan(allArguments)).ifPresent(v -> v.log(t));
    }

    private AbstractSpan getAsyncSpan(Object[] allArguments) {
        Result results = (Result) allArguments[1];
        if (results == null) {
            return null;
        }
        Request request = results.getRequest();
        if (request == null) {
            return null;
        }

        return (AbstractSpan) request.getAttributes().get(Constants.SW_JETTY_EXIT_SPAN_KEY);
    }
}
