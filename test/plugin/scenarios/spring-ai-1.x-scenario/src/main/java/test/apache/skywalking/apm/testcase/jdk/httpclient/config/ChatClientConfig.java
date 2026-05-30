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

package test.apache.skywalking.apm.testcase.jdk.httpclient.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient openAIChatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }

    @Bean
    @Lazy
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        List<Document> documentList = new ArrayList<>();
        documentList.add(new Document("The 2025 AI Summit is scheduled for October 10-12 in San Francisco. "
                + "The event will focus on Generative AI and Autonomous Agents."));
        documentList.add(new Document("Apache SkyWalking is an open-source Application Performance Management system "
                + "designed for microservices, cloud native, and container-based architectures."));
        documentList.add(new Document("Spring AI provides a unified interface for interacting with different "
                + "AI Models, allowing developers to switch between providers with minimal code changes."));
        documentList.add(new Document("A new distributed tracing protocol, TraceContext v2, was proposed "
                + "on August 25, 2025, to improve cross-cloud observability."));

        vectorStore.add(documentList);
        return vectorStore;
    }
}
