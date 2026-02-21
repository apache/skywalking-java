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

package org.apache.skywalking.apm.plugin.lettuce.common;

import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import reactor.core.CoreSubscriber;

import java.lang.reflect.Method;

/**
 * Interceptor for {@code RedisPublisher.RedisSubscription#subscribe(Subscriber)} method.
 *
 * <p>
 * This interceptor works together with the constructor interceptor of
 * {@code RedisSubscription}:
 * </p>
 */
public class RedisSubscriptionSubscribeMethodInterceptor implements InstanceMethodsAroundInterceptorV2 {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInvocationContext context) {
        if (allArguments[0] instanceof CoreSubscriber) {
            CoreSubscriber<?> subscriber = (CoreSubscriber<?>) allArguments[0];
            // get ContextSnapshot from reactor context,  the snapshot is set to reactor context by any other plugin
            // such as DispatcherHandlerHandleMethodInterceptor in spring-webflux-5.x-plugin
            Object skywalkingContextSnapshot = subscriber.currentContext().getOrDefault("SKYWALKING_CONTEXT_SNAPSHOT", null);
            if (skywalkingContextSnapshot != null) {
                ((EnhancedInstance) objInst.getSkyWalkingDynamicField()).setSkyWalkingDynamicField(new RedisCommandEnhanceInfo()
                        .setSnapshot((ContextSnapshot) skywalkingContextSnapshot));
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
    }
}
