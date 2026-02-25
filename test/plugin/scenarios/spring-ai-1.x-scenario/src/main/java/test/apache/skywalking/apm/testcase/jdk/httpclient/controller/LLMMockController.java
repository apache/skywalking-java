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
 */

package test.apache.skywalking.apm.testcase.jdk.httpclient.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

@RestController
@RequestMapping("/llm")
public class LLMMockController {
    @RequestMapping("/v1/chat/completions")
    public Object completions(@RequestBody JSONObject request, HttpServletResponse response) throws IOException {
        Boolean isStream = request.getBoolean("stream");
        if (isStream == null) isStream = false;

        JSONArray messages = request.getJSONArray("messages");
        JSONObject lastMessage = messages.getJSONObject(messages.size() - 1);
        String lastRole = lastMessage.getString("role");

        if (isStream) {
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            PrintWriter writer = response.getWriter();
            String id = "chatcmpl-fc1b64d3";
            long created = Instant.now().getEpochSecond();
            String model = "gpt-4.1-2025-04-14";

            try {
                if ("tool".equals(lastRole)) {
                    String fullContent = "The weather in New York is currently sunny with a temperature of 10°C.";
                    writeStreamChunk(writer, id, created, model, "{\"role\":\"assistant\"}", "null");

                    int len = fullContent.length();
                    String[] parts = {fullContent.substring(0, len / 3), fullContent.substring(len / 3, len * 2 / 3), fullContent.substring(len * 2 / 3)};

                    for (String part : parts) {
                        Thread.sleep(50);
                        writeStreamChunk(writer, id, created, model, "{\"content\":\"" + escapeJson(part) + "\"}", "null");
                    }

                    writeStreamChunk(writer, id, created, model, "{}", "\"stop\"");
                } else {
                    writeStreamChunk(writer, id, created, model, "{\"role\":\"assistant\"}", "null");

                    String toolCallDelta = """
                            {
                                "tool_calls": [
                                    {
                                        "index": 0,
                                        "id": "call_iV4bvFIZujbb",
                                        "type": "function",
                                        "function": {
                                            "name": "get_weather",
                                            "arguments": ""
                                        }
                                    }
                                ]
                            }
                            """;
                    writeStreamChunk(writer, id, created, model, toolCallDelta, "null");

                    String args = "{\\\"arg0\\\":\\\"new york\\\"}";
                    String argsDelta = """
                            {
                                "tool_calls": [
                                    {
                                        "index": 0,
                                        "function": {
                                            "arguments": "%s"
                                        }
                                    }
                                ]
                            }
                            """.formatted(args);
                    Thread.sleep(50);
                    writeStreamChunk(writer, id, created, model, argsDelta, "null");

                    writeStreamChunk(writer, id, created, model, "{}", "\"tool_calls\"");
                }

                writer.write("data: [DONE]\n\n");
                writer.flush();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }

        String toolCallResponse = """
                {
                    "choices": [
                        {
                            "finish_reason": "tool_calls",
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": null,
                                "tool_calls": [
                                    {
                                        "function": {
                                            "arguments": "{\\"arg0\\":\\"new york\\"}",
                                            "name": "get_weather"
                                        },
                                        "id": "call_iV4bvFIZujbb",
                                        "type": "function"
                                    }
                                ]
                            }
                        }
                    ],
                    "created": 1768490813,
                    "id": "chatcmpl-CyJXJt7gxwDgz",
                    "usage": {
                        "completion_tokens": 17,
                        "completion_tokens_details": {
                            "accepted_prediction_tokens": 0,
                            "audio_tokens": 0,
                            "reasoning_tokens": 0,
                            "rejected_prediction_tokens": 0
                        },
                        "prompt_tokens": 52,
                        "prompt_tokens_details": {
                            "audio_tokens": 0,
                            "cached_tokens": 0
                        },
                        "total_tokens": 69
                    },
                    "model": "gpt-4.1-2025-04-14",
                    "object": "chat.completion"
                }
                """;

        String finalResponse = """
                {
                   "choices": [
                       {
                           "finish_reason": "stop",
                           "index": 0,
                           "message": {
                               "content": "The weather in New York is currently sunny with a temperature of 10°C.",
                               "role": "assistant"
                           }
                       }
                   ],
                   "created": 1768491057,
                   "id":"chatcmpl-CyJXJt7gxwDgz",
                   "model": "gpt-4.1-2025-04-14",
                   "object": "chat.completion"
                }
                """;

        if ("tool".equals(lastRole)) {
            return JSON.parseObject(finalResponse);
        }

        return JSON.parseObject(toolCallResponse);
    }

    private void writeStreamChunk(PrintWriter writer, String id, long created, String model, String delta, String finishReason) {
        String json = """
                {
                    "choices": [
                        {
                            "delta": %s,
                            "finish_reason": %s,
                            "index": 0,
                            "logprobs": null
                        }
                    ],
                    "object": "chat.completion.chunk",
                    "usage": {
                        "completion_tokens": 17,
                        "completion_tokens_details": {
                            "accepted_prediction_tokens": 0,
                            "audio_tokens": 0,
                            "reasoning_tokens": 0,
                            "rejected_prediction_tokens": 0
                        },
                        "prompt_tokens": 52,
                        "prompt_tokens_details": {
                            "audio_tokens": 0,
                            "cached_tokens": 0
                        },
                        "total_tokens": 69
                    },
                    "created": %d,
                    "system_fingerprint": null,
                    "model": "%s",
                    "id": "%s"
                }
                """.formatted(delta, finishReason, created, model, id);

        String cleanJson = json.replace("\n", "").replace("\r", "");
        writer.write("data: " + cleanJson + "\n\n");
        writer.flush();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
