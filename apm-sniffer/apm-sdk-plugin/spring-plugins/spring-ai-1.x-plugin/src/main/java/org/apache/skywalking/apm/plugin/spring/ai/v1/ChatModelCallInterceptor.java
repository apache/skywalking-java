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
import org.apache.skywalking.apm.plugin.spring.ai.v1.common.ChatModelMetadataResolver;
import org.apache.skywalking.apm.plugin.spring.ai.v1.config.SpringAiPluginConfig;
import org.apache.skywalking.apm.plugin.spring.ai.v1.contant.Constants;
import org.apache.skywalking.apm.plugin.spring.ai.v1.messages.InputMessages;
import org.apache.skywalking.apm.plugin.spring.ai.v1.messages.OutputMessages;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.lang.reflect.Method;

public class ChatModelCallInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        ChatModelMetadataResolver.ApiMetadata apiMetadata = ChatModelMetadataResolver.getMetadata(objInst);
        AbstractSpan span = ContextManager.createExitSpan("Spring-ai/" + apiMetadata.getProviderName() + "/call", apiMetadata.getPeer());
        SpanLayer.asGenAI(span);
        span.setComponent(apiMetadata.getComponent());
        Tags.GEN_AI_OPERATION_NAME.set(span, Constants.CHAT);
        Tags.GEN_AI_PROVIDER_NAME.set(span, apiMetadata.getProviderName());

        Prompt prompt = (Prompt) allArguments[0];
        ChatOptions chatOptions = prompt.getOptions();
        if (chatOptions == null) {
            return;
        }

        if (chatOptions.getModel() != null) {
            Tags.GEN_AI_REQUEST_MODEL.set(span, chatOptions.getModel());
        }
        if (chatOptions.getTemperature() != null) {
            Tags.GEN_AI_TEMPERATURE.set(span, String.valueOf(chatOptions.getTemperature()));
        }
        if (chatOptions.getTopK() != null) {
            Tags.GEN_AI_TOP_K.set(span, String.valueOf(chatOptions.getTopK()));
        }
        if (chatOptions.getTopP() != null) {
            Tags.GEN_AI_TOP_P.set(span, String.valueOf(chatOptions.getTopP()));
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (!ContextManager.isActive()) {
            return ret;
        }

        try {
            if (!(ret instanceof ChatResponse)) {
                return ret;
            }

            ChatResponse response = (ChatResponse) ret;

            AbstractSpan span = ContextManager.activeSpan();
            ChatResponseMetadata metadata = response.getMetadata();

            long totalTokens = 0;

            if (metadata != null) {
                if (metadata.getId() != null) {
                    Tags.GEN_AI_RESPONSE_ID.set(span, metadata.getId());
                }
                if (metadata.getModel() != null) {
                    Tags.GEN_AI_RESPONSE_MODEL.set(span, metadata.getModel());
                }

                Usage usage = metadata.getUsage();
                if (usage != null) {
                    if (usage.getPromptTokens() != null) {
                        Tags.GEN_AI_USAGE_INPUT_TOKENS.set(span, String.valueOf(usage.getPromptTokens()));
                    }
                    if (usage.getCompletionTokens() != null) {
                        Tags.GEN_AI_USAGE_OUTPUT_TOKENS.set(span, String.valueOf(usage.getCompletionTokens()));
                    }
                    if (usage.getTotalTokens() != null) {
                        totalTokens = usage.getTotalTokens();
                        Tags.GEN_AI_CLIENT_TOKEN_USAGE.set(span, String.valueOf(totalTokens));
                    }
                }
            }

            Generation generation = response.getResult();
            if (generation != null && generation.getMetadata() != null) {
                String finishReason = generation.getMetadata().getFinishReason();
                if (finishReason != null) {
                    Tags.GEN_AI_RESPONSE_FINISH_REASONS.set(span, finishReason);
                }
            }

            collectContent(span, allArguments, response, totalTokens);
        } finally {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }

    private void collectContent(AbstractSpan span, Object[] allArguments, ChatResponse response, long totalTokens) {
        int tokenThreshold = SpringAiPluginConfig.Plugin.SpringAi.CONTENT_COLLECT_THRESHOLD_TOKENS;

        if (tokenThreshold >= 0 && totalTokens < tokenThreshold) {
            return;
        }

        if (SpringAiPluginConfig.Plugin.SpringAi.COLLECT_INPUT_MESSAGES) {
            collectPrompt(span, allArguments);
        }

        if (SpringAiPluginConfig.Plugin.SpringAi.COLLECT_OUTPUT_MESSAGES) {
            collectCompletion(span, response);
        }
    }

    private void collectPrompt(AbstractSpan span, Object[] allArguments) {
        Prompt prompt = (Prompt) allArguments[0];
        if (prompt == null) {
            return;
        }

        InputMessages inputMessages = InputMessages.fromPrompt(prompt);
        String inputMessagesJson = inputMessages.toJson();
        int limit = SpringAiPluginConfig.Plugin.SpringAi.INPUT_MESSAGES_LENGTH_LIMIT;
        if (limit > 0 && inputMessagesJson.length() > limit) {
            inputMessagesJson = inputMessagesJson.substring(0, limit);
        }

        Tags.GEN_AI_INPUT_MESSAGES.set(span, inputMessagesJson);
    }

    private void collectCompletion(AbstractSpan span, ChatResponse response) {

        OutputMessages outputMessages = OutputMessages.fromChatResponse(response);
        String outputMessagesJson = outputMessages.toJson();
        int limit = SpringAiPluginConfig.Plugin.SpringAi.OUTPUT_MESSAGES_LENGTH_LIMIT;

        if (limit > 0 && outputMessagesJson.length() > limit) {
            outputMessagesJson = outputMessagesJson.substring(0, limit);
        }
        Tags.GEN_AI_OUTPUT_MESSAGES.set(span, outputMessagesJson);
    }
}
