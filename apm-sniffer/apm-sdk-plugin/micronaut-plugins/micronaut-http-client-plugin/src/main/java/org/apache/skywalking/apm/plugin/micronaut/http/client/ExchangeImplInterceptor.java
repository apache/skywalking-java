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

package org.apache.skywalking.apm.plugin.micronaut.http.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.net.URI;

import static org.apache.skywalking.apm.plugin.micronaut.http.client.MicronautCommons.finish;
import static org.apache.skywalking.apm.plugin.micronaut.http.client.MicronautCommons.buildTracePublisher;
import static org.apache.skywalking.apm.plugin.micronaut.http.client.MicronautCommons.beginTrace;

public class ExchangeImplInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        MutableHttpRequest<?> request = (MutableHttpRequest<?>) allArguments[2];
        URI requestURI = (URI) allArguments[0];
        beginTrace(request, requestURI);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        MutableHttpRequest<?> request = (MutableHttpRequest<?>) allArguments[2];
        Publisher<HttpResponse<?>> retPublisher = (Publisher<HttpResponse<?>>) ret;
        return buildTracePublisher(request, retPublisher);
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        finish((MutableHttpRequest<?>) allArguments[0], span -> span.errorOccurred().log(t));
    }

}
