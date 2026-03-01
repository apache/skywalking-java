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
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
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
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ChatModelStreamInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        ChatModelMetadataResolver.ApiMetadata apiMetadata = ChatModelMetadataResolver.getMetadata(objInst);
        AbstractSpan span = ContextManager.createExitSpan("Spring-ai/" + apiMetadata.getProviderName() + "/stream", apiMetadata.getPeer());
        SpanLayer.asGenAI(span);

        span.setComponent(apiMetadata.getComponent());
        Tags.GEN_AI_OPERATION_NAME.set(span, Constants.CHAT);
        Tags.GEN_AI_PROVIDER_NAME.set(span, apiMetadata.getProviderName());

        Prompt prompt = (Prompt) allArguments[0];
        if (prompt == null) {
            return;
        }

        ChatOptions chatOptions = prompt.getOptions();
        if (chatOptions == null) {
            return;
        }

        Tags.GEN_AI_REQUEST_MODEL.set(span, chatOptions.getModel());
        Tags.GEN_AI_TEMPERATURE.set(span, String.valueOf(chatOptions.getTemperature()));
        Tags.GEN_AI_TOP_K.set(span, String.valueOf(chatOptions.getTopK()));
        Tags.GEN_AI_TOP_P.set(span, String.valueOf(chatOptions.getTopP()));

        ContextManager.getRuntimeContext().put(Constants.SPRING_AI_STREAM_START_TIME, System.currentTimeMillis());
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (!ContextManager.isActive()) {
            return ret;
        }

        final AbstractSpan span = ContextManager.activeSpan();
        final ContextSnapshot snapshot = ContextManager.capture();

        span.prepareForAsync();
        ContextManager.stopSpan();

        @SuppressWarnings("unchecked") final Flux<ChatResponse> flux = (Flux<ChatResponse>) ret;

        final StreamState state = new StreamState(readAndClearStartTime());

        return flux
                .doOnNext(response -> onStreamNext(span, response, state))
                .doOnError(span::log)
                .doFinally(signalType -> onStreamFinally(span, allArguments, state))
                .contextWrite(c -> c.put(Constants.SKYWALKING_CONTEXT_SNAPSHOT, snapshot));
    }

    private void onStreamNext(AbstractSpan span, ChatResponse response, StreamState state) {
        state.lastResponseRef.set(response);

        final Generation generation = response.getResult();
        if (generation == null) {
            return;
        }

        recordTtfrIfFirstToken(span, generation, state);
        recordFinishReason(generation, state);
        appendCompletionChunk(generation, state);
    }

    private void onStreamFinally(AbstractSpan span, Object[] allArguments, StreamState state) {
        try {
            ChatResponse finalResponse = state.lastResponseRef.get();
            long totalTokens = 0;

            if (finalResponse != null && finalResponse.getMetadata() != null) {
                ChatResponseMetadata metadata = finalResponse.getMetadata();
                collectResponseTags(span, metadata, state);
                totalTokens = collectUsageTags(span, metadata.getUsage());
            }

            int tokenThreshold = SpringAiPluginConfig.Plugin.SpringAi.CONTENT_COLLECT_THRESHOLD_TOKENS;
            if (tokenThreshold >= 0 && totalTokens < tokenThreshold) {
                return;
            }

            if (SpringAiPluginConfig.Plugin.SpringAi.COLLECT_INPUT_MESSAGES) {
                collectPrompt(span, allArguments);
            }

            if (SpringAiPluginConfig.Plugin.SpringAi.COLLECT_OUTPUT_MESSAGES) {
                collectCompletion(span, state);
            }
        } catch (Throwable t) {
            span.log(t);
        } finally {
            span.asyncFinish();
        }
    }

    private void recordTtfrIfFirstToken(AbstractSpan span, Generation generation, StreamState state) {
        if (state.startTime == null) {
            return;
        }
        if (generation.getOutput() == null || !StringUtils.hasText(generation.getOutput().getText())) {
            return;
        }
        if (state.firstResponseReceived.compareAndSet(false, true)) {
            Tags.GEN_AI_STREAM_TTFR.set(span, String.valueOf(System.currentTimeMillis() - state.startTime));
        }
    }

    private void recordFinishReason(Generation generation, StreamState state) {
        if (generation.getMetadata() == null) {
            return;
        }
        String reason = generation.getMetadata().getFinishReason();
        if (reason != null) {
            state.finishReason.set(reason);
        }
    }

    private void appendCompletionChunk(Generation generation, StreamState state) {
        if (generation.getOutput() == null) {
            return;
        }
        String text = generation.getOutput().getText();
        if (text != null) {
            state.completionBuilder.append(text);
        }
    }

    private void collectResponseTags(AbstractSpan span, ChatResponseMetadata metadata, StreamState state) {
        if (metadata.getId() != null) {
            Tags.GEN_AI_RESPONSE_ID.set(span, metadata.getId());
        }
        if (metadata.getModel() != null) {
            Tags.GEN_AI_RESPONSE_MODEL.set(span, metadata.getModel());
        }
        Tags.GEN_AI_RESPONSE_FINISH_REASONS.set(span, state.finishReason.get());
    }

    private long collectUsageTags(AbstractSpan span, Usage usage) {
        if (usage == null) {
            return 0;
        }

        if (usage.getPromptTokens() != null) {
            Tags.GEN_AI_USAGE_INPUT_TOKENS.set(span, String.valueOf(usage.getPromptTokens()));
        }

        if (usage.getCompletionTokens() != null) {
            Tags.GEN_AI_USAGE_OUTPUT_TOKENS.set(span, String.valueOf(usage.getCompletionTokens()));
        }

        long total = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0;
        Tags.GEN_AI_CLIENT_TOKEN_USAGE.set(span, String.valueOf(total));
        return total;
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

    private void collectCompletion(AbstractSpan span, StreamState state) {

        String fullText = state.completionBuilder.toString();
        String finishReason = state.finishReason.get();
        List<InputMessages.MessagePart> parts = new ArrayList<>();
        if (fullText != null && !fullText.isEmpty()) {
            parts.add(new InputMessages.TextPart(fullText));
        }

        OutputMessages outputMessages = OutputMessages.create().append(OutputMessages.OutputMessage.create("assistant", parts, finishReason));
        String outputMessagesJson = outputMessages.toJson();

        int limit = SpringAiPluginConfig.Plugin.SpringAi.OUTPUT_MESSAGES_LENGTH_LIMIT;
        if (limit > 0 && outputMessagesJson.length() > limit) {
            outputMessagesJson = outputMessagesJson.substring(0, limit);
        }

        Tags.GEN_AI_OUTPUT_MESSAGES.set(span, outputMessagesJson);
    }

    private Long readAndClearStartTime() {
        Long startTime = (Long) ContextManager.getRuntimeContext().get(Constants.SPRING_AI_STREAM_START_TIME);
        ContextManager.getRuntimeContext().remove(Constants.SPRING_AI_STREAM_START_TIME);
        return startTime;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }

    private static final class StreamState {
        final AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();
        final StringBuilder completionBuilder = new StringBuilder();
        final AtomicReference<String> finishReason = new AtomicReference<>("");
        final AtomicBoolean firstResponseReceived = new AtomicBoolean(false);
        final Long startTime;

        StreamState(Long startTime) {
            this.startTime = startTime;
        }
    }
}
