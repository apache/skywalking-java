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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InputMessages {

    private final List<InputMessage> messages = new ArrayList<>();

    public static InputMessages create() {
        return new InputMessages();
    }

    public InputMessages append(InputMessage message) {
        this.messages.add(message);
        return this;
    }

    public String toJson() {
        return GsonUtil.toJson(
                messages.stream()
                        .map(InputMessage::toMap)
                        .collect(Collectors.toList())
        );
    }

    public static class InputMessage {
        private final String role;
        private final List<MessagePart> parts;

        private InputMessage(String role, List<MessagePart> parts) {
            this.role = role;
            this.parts = parts;
        }

        public static InputMessage create(String role, List<MessagePart> parts) {
            return new InputMessage(role, parts);
        }

        public Map<String, Object> toMap() {
            List<Map<String, Object>> partMaps = parts.stream()
                    .map(MessagePart::toMap)
                    .collect(Collectors.toList());

            Map<String, Object> map = new HashMap<>();
            map.put("role", role != null ? role : "unknown");
            map.put("parts", partMaps);
            return map;
        }
    }

    public interface MessagePart {
        Map<String, Object> toMap();
    }

    public static class TextPart implements MessagePart {
        private final String content;

        public TextPart(String content) {
            this.content = content;
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of("type", "text", "content", content != null ? content : "");
        }
    }

    public static class ToolCallPart implements MessagePart {
        private final String id;
        private final String name;
        private final String arguments;

        public ToolCallPart(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("type", "tool_call");
            if (id != null) {
                map.put("id", id);
            }
            map.put("name", name != null ? name : "");
            map.put("arguments", arguments != null ? arguments : "");
            return map;
        }
    }

    public static class ToolCallResponsePart implements MessagePart {
        private final String id;
        private final String result;

        public ToolCallResponsePart(String id, String result) {
            this.id = id;
            this.result = result;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "tool_call_response");
            if (id != null) {
                map.put("id", id);
            }
            map.put("result", result != null ? result : "");
            return map;
        }
    }

    public static InputMessages fromPrompt(Prompt prompt) {
        InputMessages inputMessages = InputMessages.create();

        if (prompt == null || prompt.getInstructions() == null) {
            return inputMessages;
        }

        for (Message message : prompt.getInstructions()) {
            MessageType type = message.getMessageType();

            switch (type) {
                case SYSTEM:
                    inputMessages.append(InputMessage.create(
                            type.getValue(),
                            textParts(message.getText())
                    ));
                    break;

                case USER:
                    inputMessages.append(InputMessage.create(
                            type.getValue(),
                            userMessageParts(message)
                    ));
                    break;

                case ASSISTANT:
                    inputMessages.append(InputMessage.create(
                            type.getValue(),
                            assistantMessageParts(message)
                    ));
                    break;

                case TOOL:
                    inputMessages.append(InputMessage.create(
                            type.getValue(),
                            toolMessageParts(message)
                    ));
                    break;
                default:
                    inputMessages.append(InputMessage.create(
                            type.getValue(),
                            textParts(message.getText())
                    ));
                    break;
            }
        }

        return inputMessages;
    }

    private static List<MessagePart> textParts(String text) {
        List<MessagePart> parts = new ArrayList<>();
        if (text != null && !text.isEmpty()) {
            parts.add(new TextPart(text));
        }
        return parts;
    }

    private static List<MessagePart> userMessageParts(Message message) {
        List<MessagePart> parts = new ArrayList<>();

        String text = message.getText();
        if (text != null && !text.isEmpty()) {
            parts.add(new TextPart(text));
        }

        if (message instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) message;
            if (userMessage.getMedia() != null) {
                for (Media media : userMessage.getMedia()) {
                    parts.add(new TextPart("[media: " + media.getMimeType() + "]"));
                }
            }
        }

        return parts;
    }

    private static List<MessagePart> assistantMessageParts(Message message) {
        List<MessagePart> parts = new ArrayList<>();

        String text = message.getText();
        if (text != null && !text.isEmpty()) {
            parts.add(new TextPart(text));
        }

        if (message instanceof AssistantMessage) {
            AssistantMessage assistantMessage = (AssistantMessage) message;
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
            if (toolCalls != null) {
                for (AssistantMessage.ToolCall toolCall : toolCalls) {
                    parts.add(new ToolCallPart(
                            toolCall.id(),
                            toolCall.name(),
                            toolCall.arguments()
                    ));
                }
            }
        }

        return parts;
    }

    private static List<MessagePart> toolMessageParts(Message message) {
        List<MessagePart> parts = new ArrayList<>();

        if (message instanceof ToolResponseMessage) {
            ToolResponseMessage toolResponse = (ToolResponseMessage) message;
            List<ToolResponseMessage.ToolResponse> responses = toolResponse.getResponses();
            if (responses != null) {
                for (ToolResponseMessage.ToolResponse response : responses) {
                    parts.add(new ToolCallResponsePart(
                            response.id(),
                            response.responseData()
                    ));
                }
            }
        } else {
            String text = message.getText();
            if (text != null && !text.isEmpty()) {
                parts.add(new ToolCallResponsePart(null, text));
            }
        }

        return parts;
    }
}
