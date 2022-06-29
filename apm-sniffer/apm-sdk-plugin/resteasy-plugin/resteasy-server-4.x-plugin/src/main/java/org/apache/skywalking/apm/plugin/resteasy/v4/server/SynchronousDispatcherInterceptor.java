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

package org.apache.skywalking.apm.plugin.resteasy.v4.server;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

import java.lang.reflect.Method;

public class SynchronousDispatcherInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog LOG = LogManager.getLogger(SynchronousDispatcherInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        HttpRequest request = (HttpRequest) allArguments[0];

        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(request.getHttpHeaders().getHeaderString(next.getHeadKey()));
        }

        AbstractSpan span = ContextManager.createEntrySpan(request.getHttpMethod() + ":" + request.getUri().getPath(), contextCarrier);
        System.out.println(allArguments[0].getClass().getName());
        if (allArguments.length > 1) {
            System.out.println(allArguments[1].getClass().getName());
        }
        System.out.println(request.getUri().getBaseUri().getPath());
        System.out.println(request.getUri().getPath());
        System.out.println(request.getUri().getRequestUri().getPath());
        System.out.println(request.getUri().getAbsolutePath().getPath());
        span.tag(Tags.URL, toPath(request.getUri().getRequestUri().toString()));
        span.tag(Tags.HTTP.METHOD, request.getHttpMethod());
        span.setComponent(ComponentsDefine.RESTEASY);
        SpanLayer.asHttp(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        HttpResponse response = (HttpResponse) allArguments[1];
        AbstractSpan span = ContextManager.activeSpan();
        if (response.getStatus() >= 400) {
            span.errorOccurred();
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, response.getStatus());
        }
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private static String toPath(String uri) {
        int index = uri.indexOf("?");
        if (index > -1) {
            return uri.substring(0, index);
        } else {
            return uri;
        }
    }
}
