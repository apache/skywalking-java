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

package org.apache.skywalking.apm.plugin.spring.ai.v1;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.util.GsonUtil;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.spring.ai.v1.contant.Constants;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AbstractObservationVectorStoreInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        SearchRequest request = (SearchRequest) allArguments[0];
        VectorStoreObservationContext context = createObservationContext(objInst, request);
        String dataSourceId = resolveDataSourceId(context, objInst);

        AbstractSpan span = ContextManager.createExitSpan(Constants.RETRIEVAL + " " + dataSourceId, dataSourceId);
        SpanLayer.asGenAI(span);
        span.setComponent(ComponentsDefine.SPRING_AI);
        Tags.GEN_AI_OPERATION_NAME.set(span, Constants.RETRIEVAL);
        Tags.GEN_AI_PROVIDER_NAME.set(span, context.getDatabaseSystem());
        Tags.GEN_AI_DATA_SOURCE_ID.set(span, dataSourceId);
        String model = resolveEmbeddingModelName(objInst);
        if (StringUtils.hasText(model)) {
            Tags.GEN_AI_REQUEST_MODEL.set(span, model);
        }

        if (request != null) {
            Tags.GEN_AI_TOP_K.set(span, String.valueOf(request.getTopK()));
            if (StringUtils.hasText(request.getQuery())) {
                Tags.GEN_AI_RETRIEVAL_QUERY_TEXT.set(span, request.getQuery());
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        if (!ContextManager.isActive()) {
            return ret;
        }
        try {
            if (ret instanceof List<?>) {
                Tags.GEN_AI_RETRIEVAL_DOCUMENTS.set(ContextManager.activeSpan(), toDocumentsJson((List<?>) ret));
            }
        } finally {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }

    private VectorStoreObservationContext createObservationContext(EnhancedInstance objInst, SearchRequest request) {
        VectorStoreObservationContext.Builder builder = ((AbstractObservationVectorStore) objInst)
                .createObservationContextBuilder(VectorStoreObservationContext.Operation.QUERY.value());
        if (request != null) {
            builder.queryRequest(request);
        }
        return builder.build();
    }

    private String resolveEmbeddingModelName(EnhancedInstance objInst) {
        Object context = objInst.getSkyWalkingDynamicField();
        if (context instanceof VectorStoreEnhanceContext) {
            return ((VectorStoreEnhanceContext) context).getEmbeddingModelName();
        }
        return null;
    }

    private String resolveDataSourceId(VectorStoreObservationContext context, EnhancedInstance objInst) {
        if (StringUtils.hasText(context.getCollectionName())) {
            return context.getCollectionName();
        }
        if (StringUtils.hasText(context.getNamespace())) {
            return context.getNamespace();
        }
        if (StringUtils.hasText(context.getDatabaseSystem())) {
            return context.getDatabaseSystem();
        }
        return objInst.getClass().getSimpleName();
    }

    private String toDocumentsJson(List<?> documents) {
        List<Map<String, Object>> retrievalDocuments = new ArrayList<>(documents.size());
        for (Object item : documents) {
            if (!(item instanceof Document)) {
                continue;
            }
            Document document = (Document) item;
            Map<String, Object> documentMap = new LinkedHashMap<>();
            documentMap.put("id", document.getId());
            if (document.getScore() != null) {
                documentMap.put("score", document.getScore());
            }
            retrievalDocuments.add(documentMap);
        }
        return GsonUtil.toJson(retrievalDocuments);
    }
}
