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

package org.apache.skywalking.apm.plugin.baidu.brpc3;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

/**
 * brpc3 client interceptor
 */
public class ClientInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {
    @Override
    public void onConstruct(EnhancedInstance enhancedInstance, Object[] objects) throws Throwable {
        ServiceInstance url = (ServiceInstance) objects[0];
        enhancedInstance.setSkyWalkingDynamicField(url.getIp() + ":" + url.getPort());
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String peer = (String) objInst.getSkyWalkingDynamicField();
        Request request = (Request) allArguments[0];
        String operationName = generateOperationName(request);
        final ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(operationName, contextCarrier, peer);

        CarrierItem next = contextCarrier.items();
        if (request.getKvAttachment() == null) {
            request.setKvAttachment(new HashMap<>());
        }
        while (next.hasNext()) {
            next = next.next();
            request.getKvAttachment().put(next.getHeadKey(), next.getHeadValue());
        }
        span.setComponent(ComponentsDefine.BRPC_JAVA);
        SpanLayer.asRPCFramework(span);
        Tags.URL.set(span, peer + "/" + operationName);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        Response response = (Response) allArguments[1];
        if (response != null && response.getException() != null) {
            dealException(response.getException());
        }

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        dealException(t);
    }

    private void dealException(Throwable throwable) {
        AbstractSpan span = ContextManager.activeSpan();
        span.log(throwable);
    }

    private String generateOperationName(Request request) {
        StringBuilder operationName = new StringBuilder();
        operationName.append(request.getServiceName() + "." + request.getMethodName());
        return operationName.toString();
    }

}
