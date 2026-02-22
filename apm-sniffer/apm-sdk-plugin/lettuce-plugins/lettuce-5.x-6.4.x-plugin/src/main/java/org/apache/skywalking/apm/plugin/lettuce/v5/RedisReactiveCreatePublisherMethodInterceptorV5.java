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

package org.apache.skywalking.apm.plugin.lettuce.v5;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Intercepts reactive publisher factory methods (createMono/createFlux)
 * to ensure the SkyWalking context snapshot is propagated via Reactor Context.
 *
 * <p>If the Reactor Context does not already contain a snapshot, this interceptor
 * captures the current active context and writes it into the subscriber context
 * as a fallback propagation mechanism.</p>
 */
public class RedisReactiveCreatePublisherMethodInterceptorV5 implements InstanceMethodsAroundInterceptorV2 {

    private static final String SNAPSHOT_KEY = "SKYWALKING_CONTEXT_SNAPSHOT";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInvocationContext context) {
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) {

        if (!(ret instanceof Mono) && !(ret instanceof Flux)) {
            return ret;
        }

        final ContextSnapshot snapshot;
        if (ContextManager.isActive()) {
            snapshot = ContextManager.capture();
        } else {
            return ret;
        }

        Function<Context, Context> contextFunction = ctx -> {
            if (ctx.hasKey(SNAPSHOT_KEY)) {
                return ctx;
            }
            return ctx.put(SNAPSHOT_KEY, snapshot);
        };

        if (ret instanceof Mono) {
            Mono<?> original = (Mono<?>) ret;
            return original.subscriberContext(contextFunction);
        } else {
            Flux<?> original = (Flux<?>) ret;
            return original.subscriberContext(contextFunction);
        }
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
    }
}
