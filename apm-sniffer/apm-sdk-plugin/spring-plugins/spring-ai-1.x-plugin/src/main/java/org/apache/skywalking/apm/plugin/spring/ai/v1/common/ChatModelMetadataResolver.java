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

package org.apache.skywalking.apm.plugin.spring.ai.v1.common;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.network.trace.component.OfficialComponent;
import org.apache.skywalking.apm.plugin.spring.ai.v1.enums.AiProviderEnum;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class ChatModelMetadataResolver {

    private static final ILog LOGGER = LogManager.getLogger(ChatModelMetadataResolver.class);

    private static final Map<String, ApiMetadata> MODEL_METADATA_MAP = new HashMap<>();

    static {
        for (AiProviderEnum provider : AiProviderEnum.values()) {
            if (provider.getModelClassName() != null && provider.getValue() != null) {
                MODEL_METADATA_MAP.put(
                        provider.getModelClassName(),
                        new ApiMetadata(provider.getValue(), matchComponent(provider))
                );
            }
        }
    }

    private static OfficialComponent matchComponent(AiProviderEnum provider) {
        switch (provider) {
            case ANTHROPIC_CLAUDE:
                return ComponentsDefine.SPRING_AI_ANTHROPIC;
            case AMAZON_BEDROCK_CONVERSE:
                return ComponentsDefine.SPRING_AI_BEDROCK;
            case AZURE_OPENAI:
                return ComponentsDefine.SPRING_AI_AZURE_OPENAI;
            case OCI_GENAI_COHERE:
                return ComponentsDefine.SPRING_AI_COHERE;
            case DEEPSEEK:
                return ComponentsDefine.SPRING_AI_DEEPSEEK;
            case GOOGLE_GENAI:
                return ComponentsDefine.SPRING_AI_GOOGLE_GENAI;
            case GOOGLE_VERTEXAI_GEMINI:
                return ComponentsDefine.SPRING_AI_VERTEXAI;
            case MISTRAL_AI:
                return ComponentsDefine.SPRING_AI_MISTRAL_AI;
            case OPENAI:
                return ComponentsDefine.SPRING_AI_OPENAI;
            case HUGGINGFACE:
                return ComponentsDefine.SPRING_AI_HUGGINGFACE;
            case MINIMAX:
                return ComponentsDefine.SPRING_AI_MINIMAX;
            case OLLAMA:
                return ComponentsDefine.SPRING_AI_OLLAMA;
            case OPENAI_SDK_OFFICIAL:
                return ComponentsDefine.SPRING_AI_OPENAI;
            case ZHIPU_AI:
                return ComponentsDefine.SPRING_AI_ZHIPU_AI;
            case UNKNOWN:
            default:
                return ComponentsDefine.SPRING_AI_UNKNOWN;
        }
    }

    @NotNull
    public static ApiMetadata getMetadata(Object chatModelInstance) {
        ApiMetadata metadata = MODEL_METADATA_MAP.get(chatModelInstance.getClass().getName());
        if (metadata == null) {
            MODEL_METADATA_MAP.get(AiProviderEnum.UNKNOWN);
        }
        return metadata;
    }

    public static ApiMetadata getMetadata(String modelClassName) {
        try {
            return MODEL_METADATA_MAP.get(modelClassName);
        } catch (Exception e) {
            LOGGER.error("spring-ai plugin get modelMetadata error: ", e);
            return null;
        }
    }

    public static class ApiMetadata {

        private final String providerName;
        private final OfficialComponent component;
        private volatile String baseUrl;
        private volatile String completionsPath;

        ApiMetadata(String providerName, OfficialComponent component) {
            this.providerName = providerName;
            this.component = component;
        }

        public String getProviderName() {
            return providerName;
        }

        public OfficialComponent getComponent() {
            return component;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getCompletionsPath() {
            return completionsPath;
        }

        public void setCompletionsPath(String completionsPath) {
            this.completionsPath = completionsPath;
        }

        public String getPeer() {
            if (baseUrl != null && !baseUrl.isEmpty()) {
                return completionsPath != null && !completionsPath.isEmpty()
                        ? baseUrl + completionsPath
                        : baseUrl;
            }
            return providerName;
        }
    }
}