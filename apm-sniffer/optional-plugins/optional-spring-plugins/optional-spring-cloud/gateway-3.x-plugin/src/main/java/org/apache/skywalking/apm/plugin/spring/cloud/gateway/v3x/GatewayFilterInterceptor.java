/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.SPRING_CLOUD_GATEWAY;

public class GatewayFilterInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ThreadLocal<AtomicInteger> STACK_DEEP = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        if (isEntry()) {
            ServerWebExchange exchange = (ServerWebExchange) allArguments[0];

            EnhancedInstance enhancedInstance = getInstance(exchange);

            AbstractSpan span = ContextManager.createLocalSpan("SpringCloudGateway/GatewayFilter");
            if (enhancedInstance != null && enhancedInstance.getSkyWalkingDynamicField() != null) {
                ContextManager.continued((ContextSnapshot) enhancedInstance.getSkyWalkingDynamicField());
            }
            span.setComponent(SPRING_CLOUD_GATEWAY);
        }
    }

    public static EnhancedInstance getInstance(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof DefaultServerWebExchange) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getInstance(delegate);
        }
        return instance;
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        if (isExit()) {
            if (ContextManager.isActive()) {
                ContextManager.stopSpan();
            }
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private boolean isEntry() {
        return STACK_DEEP.get().getAndIncrement() == 0;
    }

    private boolean isExit() {
        boolean isExit = STACK_DEEP.get().decrementAndGet() == 0;
        if (isExit) {
            STACK_DEEP.remove();
        }
        return isExit;
    }
}
