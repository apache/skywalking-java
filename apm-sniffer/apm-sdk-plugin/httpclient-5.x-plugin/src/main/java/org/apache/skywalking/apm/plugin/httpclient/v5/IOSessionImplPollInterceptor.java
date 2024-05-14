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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.command.RequestExecutionCommand;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.reactor.Command;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.net.URI;

public class IOSessionImplPollInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            Object ret) throws Throwable {
        Command command = (Command) ret;
        if (!(command instanceof RequestExecutionCommand)) {
            return ret;
        }
        HttpContext httpContext = ((RequestExecutionCommand) command).getContext();
        ContextSnapshot snapshot = (ContextSnapshot) httpContext.getAttribute(Constants.SKYWALKING_CONTEXT_SNAPSHOT);
        if (snapshot == null) {
            return ret;
        }
        httpContext.removeAttribute(Constants.SKYWALKING_CONTEXT_SNAPSHOT);
        AbstractSpan localSpan = ContextManager.createLocalSpan("httpasyncclient/local");
        localSpan.setComponent(ComponentsDefine.HTTP_ASYNC_CLIENT);
        localSpan.setLayer(SpanLayer.HTTP);
        ContextManager.continued(snapshot);

        final ContextCarrier contextCarrier = new ContextCarrier();
        BasicHttpRequest request = (BasicHttpRequest) httpContext.getAttribute(HttpClientContext.HTTP_REQUEST);
        URI uri = request.getUri();

        String operationName = uri.getPath();
        int port = uri.getPort();
        AbstractSpan span = ContextManager
                .createExitSpan(operationName, contextCarrier, uri.getHost() + ":" + (port == -1 ? 80 : port));
        span.setComponent(ComponentsDefine.HTTP_ASYNC_CLIENT);
        Tags.URL.set(span, uri.toURL().toString());
        Tags.HTTP.METHOD.set(span, request.getMethod());
        SpanLayer.asHttp(span);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            request.setHeader(next.getHeadKey(), next.getHeadValue());
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Throwable t) {

    }
}
