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

package org.apache.skywalking.apm.plugin.elasticsearch.v7.interceptor;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.elasticsearch.common.RestClientEnhanceInfo;
import org.apache.skywalking.apm.plugin.elasticsearch.v7.Constants;
import org.apache.skywalking.apm.plugin.elasticsearch.v7.support.AdapterUtil;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.indices.AnalyzeRequest;

import java.lang.reflect.Method;

import static org.apache.skywalking.apm.plugin.elasticsearch.v7.ElasticsearchPluginConfig.Plugin.Elasticsearch.TRACE_DSL;
import static org.apache.skywalking.apm.plugin.elasticsearch.v7.Constants.DB_TYPE;

public class IndicesClientAnalyzeMethodsInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog LOGGER = LogManager.getLogger(IndicesClientAnalyzeMethodsInterceptor.class);

    private static final AbstractTag<String> ANALYZER_TAG = Tags.ofKey("analyzer");

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        AnalyzeRequest analyzeRequest = (AnalyzeRequest) allArguments[0];

        RestClientEnhanceInfo restClientEnhanceInfo = (RestClientEnhanceInfo) objInst.getSkyWalkingDynamicField();
        if (restClientEnhanceInfo != null) {
            AbstractSpan span = ContextManager.createExitSpan(Constants.ANALYZE_OPERATOR_NAME, restClientEnhanceInfo.getPeers());
            span.setComponent(ComponentsDefine.REST_HIGH_LEVEL_CLIENT);

            Tags.DB_TYPE.set(span, DB_TYPE);
            span.tag(ANALYZER_TAG, analyzeRequest.analyzer());
            if (TRACE_DSL) {
                Tags.DB_STATEMENT.set(span, analyzeRequest.text()[0]);
            }
            SpanLayer.asDB(span);
            if (allArguments.length > 2 && allArguments[2] != null && allArguments[2] instanceof ActionListener) {
                allArguments[2] = AdapterUtil.wrapActionListener(restClientEnhanceInfo, Constants.ANALYZE_OPERATOR_NAME,
                                                                 (ActionListener) allArguments[2]);
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        RestClientEnhanceInfo restClientEnhanceInfo = (RestClientEnhanceInfo) objInst.getSkyWalkingDynamicField();
        if (restClientEnhanceInfo != null) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        RestClientEnhanceInfo restClientEnhanceInfo = (RestClientEnhanceInfo) objInst.getSkyWalkingDynamicField();
        if (restClientEnhanceInfo != null) {
            ContextManager.activeSpan().log(t);
        }
    }
}
