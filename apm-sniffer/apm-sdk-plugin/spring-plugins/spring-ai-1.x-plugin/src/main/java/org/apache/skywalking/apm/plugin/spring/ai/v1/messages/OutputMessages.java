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

package org.apache.skywalking.apm.plugin.spring.ai.v1.messages;

import org.apache.skywalking.apm.agent.core.util.GsonUtil;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OutputMessages {
    private final List<OutputMessage> messages = new ArrayList<>();

    public static OutputMessages create() {
        return new OutputMessages();
    }

    public OutputMessages append(OutputMessage message) {
        this.messages.add(message);
        return this;
    }

    public String toJson() {
        return GsonUtil.toJson(
                messages.stream()
                        .map(OutputMessage::toMap)
                        .collect(Collectors.toList())
        );
    }

    public static class OutputMessage {
        private final String role;
        private final List<InputMessages.MessagePart> parts;
        private final String finishReason;

        private OutputMessage(String role, List<InputMessages.MessagePart> parts, String finishReason) {
            this.role = role;
            this.parts = parts;
            this.finishReason = finishReason;
        }

        public static OutputMessage create(String role, List<InputMessages.MessagePart> parts, String finishReason) {
            return new OutputMessage(role, parts, finishReason);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("role", role != null ? role : "assistant");
            map.put("parts", parts.stream()
                    .map(InputMessages.MessagePart::toMap)
                    .collect(Collectors.toList()));
            if (finishReason != null && !finishReason.isEmpty()) {
                map.put("finish_reason", finishReason);
            }
            return map;
        }
    }

    public static OutputMessages fromChatResponse(ChatResponse chatResponse) {
        OutputMessages outputMessages = OutputMessages.create();

        if (chatResponse == null || chatResponse.getResults() == null) {
            return outputMessages;
        }

        for (Generation generation : chatResponse.getResults()) {
            List<InputMessages.MessagePart> messageParts = new ArrayList<>();

            AssistantMessage assistantMessage = generation.getOutput();
            if (assistantMessage != null) {
                // Text content
                String text = assistantMessage.getText();
                if (text != null && !text.isEmpty()) {
                    messageParts.add(new InputMessages.TextPart(text));
                }

                // Tool calls
                List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
                if (toolCalls != null) {
                    for (AssistantMessage.ToolCall toolCall : toolCalls) {
                        messageParts.add(new InputMessages.ToolCallPart(
                                toolCall.id(),
                                toolCall.name(),
                                toolCall.arguments()
                        ));
                    }
                }
            }

            String finishReason = "";
            if (generation.getMetadata() != null && generation.getMetadata().getFinishReason() != null) {
                finishReason = generation.getMetadata().getFinishReason().toLowerCase();
            }

            outputMessages.append(OutputMessage.create("assistant", messageParts, finishReason));
        }

        return outputMessages;
    }
}
