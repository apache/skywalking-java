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

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v412x;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.spring.cloud.gateway.v4x.define.EnhanceObjectCache;
import reactor.netty.http.client.HttpClient;

import java.lang.reflect.Method;

/**
 * Attach the context snapshot of the current request to the {@link HttpClient} returned by
 * <code>NettyRoutingFilter#getHttpClient</code>, so that the outbound span created when the reactive chain is
 * subscribed can continue the trace of the request that assembled it.
 */
public class NettyRoutingGetHttpClientV412Interceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (!ContextManager.isActive() || !(ret instanceof HttpClient)) {
            return ret;
        }
        /*
         * NettyRoutingFilter#getHttpClient returns the shared HttpClient bean itself unless a connect timeout is
         * configured, so the returned instance is the very same object for every request of the whole JVM. The
         * snapshot is not read here but later, when the chain assembled by NettyRoutingFilter#filter is subscribed,
         * therefore a concurrent request is able to overwrite it in between and the outbound sw8 header would carry
         * another request's context.
         *
         * Derive a per-request client to hold the snapshot instead. HttpClient#headers duplicates the client and
         * copies the header map only, which Spring Cloud Gateway copies once more right after, so nothing but one
         * duplication is added to a chain that already duplicates on headers(), request() and uri().
         */
        final HttpClient perRequestHttpClient = ((HttpClient) ret).headers(headers -> {
        });
        if (!(perRequestHttpClient instanceof EnhancedInstance)) {
            return ret;
        }
        final EnhanceObjectCache enhanceObjectCache = new EnhanceObjectCache();
        enhanceObjectCache.setContextSnapshot(ContextManager.capture());
        ((EnhancedInstance) perRequestHttpClient).setSkyWalkingDynamicField(enhanceObjectCache);
        return perRequestHttpClient;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
    }

}
