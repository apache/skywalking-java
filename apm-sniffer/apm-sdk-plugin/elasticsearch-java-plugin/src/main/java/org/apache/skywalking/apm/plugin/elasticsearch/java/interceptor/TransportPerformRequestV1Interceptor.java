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
 * Intercept {@code co.elastic.clients.base.rest_client.RestClientTransport.performRequest()}
 * for elasticsearch-java 7.15.x.
 * <p>
 * In 7.15.x, the Endpoint interface ({@code co.elastic.clients.base.Endpoint}) does not have
 * an {@code id()} method, so the operation name is derived from the request class name.
 * E.g., {@code IndexRequest} → {@code Elasticsearch/index}.
 * <p>
 * Args: [0] request, [1] endpoint (co.elastic.clients.base.Endpoint)
 */
public class TransportPerformRequestV1Interceptor implements InstanceMethodsAroundInterceptor {

    private static final String DB_TYPE = "Elasticsearch";
    private static final String REQUEST_SUFFIX = "Request";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Object request = allArguments[0];
        String operationName = "Elasticsearch/" + deriveOperationName(request);

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

    /**
     * Derive operation name from request class simple name.
     * E.g., IndexRequest → index, SearchRequest → search, CreateIndexRequest → create_index
     */
    private String deriveOperationName(Object request) {
        String className = request.getClass().getSimpleName();
        if (className.endsWith(REQUEST_SUFFIX)) {
            className = className.substring(0, className.length() - REQUEST_SUFFIX.length());
        }
        // Convert PascalCase to snake_case
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
