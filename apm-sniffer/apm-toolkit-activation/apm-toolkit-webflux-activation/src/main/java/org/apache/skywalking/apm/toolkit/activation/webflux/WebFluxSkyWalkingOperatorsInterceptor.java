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

package org.apache.skywalking.apm.toolkit.activation.webflux;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import reactor.util.context.Context;

import java.lang.reflect.Method;

/**
 */
public class WebFluxSkyWalkingOperatorsInterceptor implements StaticMethodsAroundInterceptor {
    
    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                             MethodInterceptResult result) {
        // get ContextSnapshot from reactor context,  the snapshot is set to reactor context by any other plugin
        // such as DispatcherHandlerHandleMethodInterceptor in spring-webflux-5.x-plugin
        if (parameterTypes[0] == Context.class) {
            ((Context) allArguments[0]).getOrEmpty("SKYWALKING_CONTEXT_SNAPSHOT")
                    .ifPresent(ctx -> {
                        ContextManager.createLocalSpan("WebFluxOperators/onNext").setComponent(ComponentsDefine.SPRING_WEBFLUX);
                        ContextManager.continued((ContextSnapshot) ctx);
                    });
        } else if (parameterTypes[0] == ServerWebExchange.class) {
            EnhancedInstance instance = getInstance(allArguments[0]);
            if (instance != null && instance.getSkyWalkingDynamicField() != null) {
                ContextManager.createLocalSpan("WebFluxOperators/onNext").setComponent(ComponentsDefine.SPRING_WEBFLUX);
                ContextManager.continued((ContextSnapshot) instance.getSkyWalkingDynamicField());
            }
        }
    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Object ret) {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                                      Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private static EnhancedInstance getInstance(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof DefaultServerWebExchange && o instanceof EnhancedInstance) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getInstance(delegate);
        }
        return instance;
    }
}
