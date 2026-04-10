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
import org.apache.skywalking.apm.plugin.elasticsearch.java.ElasticsearchPluginConfig;

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
    @SuppressWarnings("unchecked")
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Endpoint endpoint = (Endpoint) allArguments[1];
        String operationName = "Elasticsearch/" + endpoint.id();

        String peers = (String) objInst.getSkyWalkingDynamicField();
        if (peers == null || peers.isEmpty()) {
            peers = "Unknown";
        }

        AbstractSpan span = ContextManager.createExitSpan(operationName, peers);
        span.setComponent(ComponentsDefine.REST_HIGH_LEVEL_CLIENT);
        Tags.DB_TYPE.set(span, DB_TYPE);
        SpanLayer.asDB(span);

        Object request = allArguments[0];
        String requestUrl = endpoint.requestUrl(request);
        String index = extractIndex(requestUrl);
        if (index != null) {
            span.tag(Tags.ofKey("db.instance"), index);
        }
        if (ElasticsearchPluginConfig.Plugin.Elasticsearch.TRACE_DSL) {
            String dsl = request.toString();
            if (dsl != null && !dsl.isEmpty()) {
                int maxLen = ElasticsearchPluginConfig.Plugin.Elasticsearch.ELASTICSEARCH_DSL_LENGTH_THRESHOLD;
                if (maxLen > 0 && dsl.length() > maxLen) {
                    dsl = dsl.substring(0, maxLen) + "...";
                }
                Tags.DB_STATEMENT.set(span, dsl);
            }
        }
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
     * Extract index name from request URL.
     * E.g., "/test-index/_doc/1" → "test-index", "/_bulk" → null
     */
    static String extractIndex(String requestUrl) {
        if (requestUrl == null || requestUrl.isEmpty()) {
            return null;
        }
        // Remove leading slash
        String path = requestUrl.startsWith("/") ? requestUrl.substring(1) : requestUrl;
        if (path.isEmpty()) {
            return null;
        }
        // First segment before '/' or '_' prefix means no index
        int slashIdx = path.indexOf('/');
        String firstSegment = slashIdx > 0 ? path.substring(0, slashIdx) : path;
        if (firstSegment.startsWith("_")) {
            return null;
        }
        return firstSegment;
    }
}
