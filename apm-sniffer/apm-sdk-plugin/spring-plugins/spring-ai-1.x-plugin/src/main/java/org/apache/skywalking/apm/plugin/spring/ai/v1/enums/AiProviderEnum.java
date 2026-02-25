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

package org.apache.skywalking.apm.plugin.spring.ai.v1.enums;

public enum AiProviderEnum {

    UNKNOWN("unknown", null),

    ANTHROPIC_CLAUDE("anthropic", "org.springframework.ai.anthropic.AnthropicChatModel"),

    AMAZON_BEDROCK_CONVERSE("aws.bedrock", "org.springframework.ai.bedrock.converse.BedrockProxyChatModel"),

    AZURE_OPENAI("azure.openai", "org.springframework.ai.azure.openai.AzureOpenAiChatModel"),

    OCI_GENAI_COHERE("cohere", "org.springframework.ai.oci.cohere.OCICohereChatModel"),

    DEEPSEEK("deepseek", "org.springframework.ai.deepseek.DeepSeekChatModel"),

    GOOGLE_GENAI("gcp.gen_ai", "org.springframework.ai.google.genai.GoogleGenAiChatModel"),

    GOOGLE_VERTEXAI_GEMINI("gcp.vertex_ai", "org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel"),

    MISTRAL_AI("mistral_ai", "org.springframework.ai.mistralai.MistralAiChatModel"),

    OPENAI("openai", "org.springframework.ai.openai.OpenAiChatModel"),

    HUGGINGFACE("huggingface", "org.springframework.ai.huggingface.HuggingfaceChatModel"),

    MINIMAX("minimax", "org.springframework.ai.minimax.MiniMaxChatModel"),

    OLLAMA("ollama", "org.springframework.ai.ollama.OllamaChatModel"),

    OPENAI_SDK_OFFICIAL("openai", "org.springframework.ai.openaisdk.OpenAiSdkChatModel"),

    ZHIPU_AI("zhipu_ai", "org.springframework.ai.zhipuai.ZhiPuAiChatModel");

    private final String value;
    private final String modelClassName;

    AiProviderEnum(String value, String modelClassName) {
        this.value = value;
        this.modelClassName = modelClassName;
    }

    public String getValue() {
        return value;
    }

    public String getModelClassName() {
        return modelClassName;
    }
}
