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

package org.apache.skywalking.apm.plugin.grizzly.v2;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.glassfish.grizzly.http.server.Request;

public class HttpHandlerInterceptor implements InstanceMethodsAroundInterceptorV2 {

    public static final String GRIZZLY_CONTEXT = "SW_GRIZZLY_CONTEXT";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInvocationContext context) throws Throwable {
        // entry span
        Request request = (Request) allArguments[0];
        request.getHeaderNames();
        final ContextCarrier carrier = new ContextCarrier();
        CarrierItem items = carrier.items();
        while (items.hasNext()) {
            items = items.next();
            items.setHeadValue(request.getHeader(items.getHeadKey()));
        }
        final AbstractSpan span = ContextManager.createEntrySpan(request.getMethod().getMethodString() + ":" + request.getRequestURI(), carrier);
        Tags.URL.set(span, request.getRequestURL().toString());
        Tags.HTTP.METHOD.set(span, request.getMethod().getMethodString());
        span.setComponent(ComponentsDefine.GRIZZLY);
        SpanLayer.asHttp(span);
        span.prepareForAsync();
        Object[] grizzlyContext = new Object[]{span, ContextManager.capture()};
        request.setAttribute(GRIZZLY_CONTEXT, grizzlyContext);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        ContextManager.activeSpan().log(t);
    }
}
