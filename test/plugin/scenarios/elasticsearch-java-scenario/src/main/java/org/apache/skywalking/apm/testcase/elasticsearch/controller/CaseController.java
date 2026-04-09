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

package org.apache.skywalking.apm.testcase.elasticsearch.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/elasticsearch-java-case/case")
public class CaseController {

    @Value("${elasticsearch.server}")
    private String elasticsearchServer;

    @GetMapping("/healthCheck")
    public String healthCheck() throws IOException {
        RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchServer)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);
        try {
            client.info();
            return "Success";
        } finally {
            restClient.close();
        }
    }

    @GetMapping("/elasticsearch")
    public String elasticsearch() throws IOException {
        RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchServer)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);
        try {

            // Create index
            CreateIndexResponse createResp = client.indices().create(c -> c.index("test-index"));

            // Index document
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "test");
            doc.put("value", "skywalking");
            IndexResponse indexResp = client.index(i -> i.index("test-index").id("1").document(doc));

            // Get document
            GetResponse<Map> getResp = client.get(g -> g.index("test-index").id("1"), Map.class);

            // Search
            SearchResponse<Map> searchResp = client.search(s -> s
                .index("test-index")
                .query(q -> q.match(m -> m.field("name").query("test"))), Map.class);

            // Delete document
            DeleteResponse deleteResp = client.delete(d -> d.index("test-index").id("1"));

            // Delete index
            DeleteIndexResponse deleteIndexResp = client.indices().delete(d -> d.index("test-index"));

            return "Success";
        } finally {
            restClient.close();
        }
    }
}
