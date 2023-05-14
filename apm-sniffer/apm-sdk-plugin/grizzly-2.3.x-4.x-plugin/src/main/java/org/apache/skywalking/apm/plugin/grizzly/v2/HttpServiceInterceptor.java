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
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import static org.apache.skywalking.apm.plugin.grizzly.v2.HttpHandlerInterceptor.GRIZZLY_CONTEXT;

public class HttpServiceInterceptor implements InstanceMethodsAroundInterceptorV2 {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInvocationContext context) throws Throwable {
        Request request = (Request) allArguments[0];
        Object[] grizzlyContext = (Object[]) request.getAttribute(GRIZZLY_CONTEXT);
        context.setContext(grizzlyContext);
        ContextSnapshot contextSnapshot = (ContextSnapshot) grizzlyContext[1];
        AbstractSpan span = ContextManager.createLocalSpan("GrizzlyRunService");
        span.setComponent(ComponentsDefine.GRIZZLY);
        ContextManager.continued(contextSnapshot);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) throws Throwable {
        Object[] grizzlyContext = (Object[]) context.getContext();
        AbstractSpan abstractSpan = (AbstractSpan) grizzlyContext[0];
        ContextManager.stopSpan();
        Response response = (Response) allArguments[1];
        Tags.HTTP_RESPONSE_STATUS_CODE.set(abstractSpan, response.getStatus());
        if (response.getStatus() >= 400) {
            abstractSpan.errorOccurred();
        }
        abstractSpan.asyncFinish();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        ContextManager.activeSpan().log(t);
    }
}
