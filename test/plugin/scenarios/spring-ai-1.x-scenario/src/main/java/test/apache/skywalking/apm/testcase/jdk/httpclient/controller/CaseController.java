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

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.apache.skywalking.apm.testcase.jdk.httpclient.tool.WeatherTool;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/case")
@RequiredArgsConstructor
public class CaseController {

    private final WeatherTool weatherTool;
    private final ChatClient chatClient;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @GetMapping("/healthCheck")
    public String healthCheck() {
        return "Success";
    }

    @GetMapping("/spring-ai-1.x-scenario-case")
    public String testCase() throws Exception {

        String systemPrompt = """
                You are a professional technical assistant.
                Strictly use the provided context to answer questions.
                If the information is not in the context, say: "I'm sorry, I don't have that information in my knowledge base."
                Do not use outside knowledge. Be concise.
                """;

        chatClient
                .prompt("What's the weather in New York?")
                .system(systemPrompt)
                .tools(weatherTool)
                .call()
                .content();

        chatClient
                .prompt("What's the weather in New York?")
                .system(systemPrompt)
                .tools(weatherTool)
                .stream()
                .content()
                .doOnNext(System.out::println)
                .blockLast();

        String question = "What is Apache SkyWalking?";
        VectorStore vectorStore = vectorStoreProvider.getObject();
        String context = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(2)
                        .build())
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        chatClient
                .prompt(question)
                .system("Answer using only the following context:\n" + context)
                .call()
                .content();

        return "success";
    }
}
