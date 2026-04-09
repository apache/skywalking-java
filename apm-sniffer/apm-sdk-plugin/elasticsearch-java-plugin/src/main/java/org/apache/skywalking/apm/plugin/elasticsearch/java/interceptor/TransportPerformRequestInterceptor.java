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

package org.apache.skywalking.apm.plugin.elasticsearch.java.interceptor;

import co.elastic.clients.transport.Endpoint;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

/**
 * Intercept ElasticsearchTransport.performRequest() to create exit spans.
 * <p>
 * Args: [0] request, [1] endpoint (Endpoint), [2] options (TransportOptions)
 * The endpoint.id() provides the operation name (e.g., "search", "index", "bulk").
 */
public class TransportPerformRequestInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String DB_TYPE = "Elasticsearch";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Endpoint<?, ?, ?> endpoint = (Endpoint<?, ?, ?>) allArguments[1];
        String operationName = "Elasticsearch/" + endpoint.id();

        String peers = (String) objInst.getSkyWalkingDynamicField();
        if (peers == null || peers.isEmpty()) {
            peers = "Unknown";
        }

        AbstractSpan span = ContextManager.createExitSpan(operationName, peers);
        span.setComponent(ComponentsDefine.REST_HIGH_LEVEL_CLIENT);
        Tags.DB_TYPE.set(span, DB_TYPE);
        SpanLayer.asDB(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
        ContextManager.activeSpan().errorOccurred();
    }
}
