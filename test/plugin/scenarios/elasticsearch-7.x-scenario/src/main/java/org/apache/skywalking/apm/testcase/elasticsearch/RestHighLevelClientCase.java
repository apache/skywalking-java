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

package org.apache.skywalking.apm.testcase.elasticsearch;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.skywalking.apm.testcase.elasticsearch.controller.CaseController;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.recovery.RecoverySettings;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Collections.singletonMap;

@Component
public class RestHighLevelClientCase {

    private static final Logger LOGGER = LogManager.getLogger(CaseController.class);

    @Autowired
    private RestHighLevelClient client;

    public String healthCheck() throws Exception {
        ClusterHealthRequest request = new ClusterHealthRequest();
        request.timeout(TimeValue.timeValueSeconds(10));
        request.waitForStatus(ClusterHealthStatus.GREEN);

        ClusterHealthResponse response = client.cluster().health(request, RequestOptions.DEFAULT);
        if (response.isTimedOut()) {
            String message = "elastic search node start fail!";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
        return "Success";
    }

    public String elasticsearch() throws Exception {
        String indexName = UUID.randomUUID().toString();
        try {
            // health
            health();

            // get settings
            getSettings();

            // put settings
            putSettings();

            // create
            createIndex(indexName);

            // index
            index(indexName);

            client.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);

            // get
            get(indexName);

            // search
            search(indexName);

            // update
            update(indexName);

            // delete
            delete(indexName);
        } finally {
            if (null != client) {
                client.close();
            }
        }
        return "Success";
    }

    private void health() throws IOException {
        ClusterHealthRequest request = new ClusterHealthRequest();
        ClusterHealthResponse response = client.cluster().health(request, RequestOptions.DEFAULT);
        if (response.isTimedOut()) {
            String message = "elastic search health fail!";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void putSettings() throws IOException {
        ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();
        String transientSettingKey =
            RecoverySettings.INDICES_RECOVERY_MAX_BYTES_PER_SEC_SETTING.getKey();
        int transientSettingValue = 10;
        Settings transientSettings = Settings.builder()
                                             .put(transientSettingKey, transientSettingValue, ByteSizeUnit.BYTES)
                                             .build();
        request.transientSettings(transientSettings);
        ClusterUpdateSettingsResponse response = client.cluster().putSettings(request, RequestOptions.DEFAULT);
        if (response == null) {
            String message = "elasticsearch put settings fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void getSettings() throws IOException {
        ClusterGetSettingsResponse response = client.cluster()
                                                    .getSettings(
                                                        new ClusterGetSettingsRequest(),
                                                        RequestOptions.DEFAULT
                                                    );
        if (response == null) {
            String message = "elasticsearch get settings fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void createIndex(String indexName) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);

        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("properties");
            {
                builder.startObject("author");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
                builder.startObject("title");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();
        request.mapping(builder);

        request.settings(Settings.builder().put("index.number_of_shards", 1).put("index.number_of_replicas", 0));

        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
        if (!createIndexResponse.isAcknowledged()) {
            String message = "elasticsearch create index fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void index(String indexName) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.field("author", "Marker");
            builder.field("title", "Java programing.");
        }
        builder.endObject();
        IndexRequest indexRequest = new IndexRequest(indexName).id("1").source(builder);

        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        if (indexResponse.status().getStatus() >= 400) {
            String message = "elasticsearch index data fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void get(String indexName) throws IOException {
        GetRequest getRequest = new GetRequest(indexName, "1");
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);

        if (!getResponse.isExists()) {
            String message = "elasticsearch get data fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void update(String indexName) throws IOException {
        UpdateRequest request = new UpdateRequest(indexName, "1");
        Map<String, Object> parameters = singletonMap("title", "c++ programing.");
        Script inline = new Script(ScriptType.INLINE, "painless", "ctx._source.title = params.title", parameters);
        request.script(inline);

        UpdateResponse updateResponse = client.update(request, RequestOptions.DEFAULT);
        if (updateResponse.getVersion() != 2) {
            String message = "elasticsearch update data fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void delete(String indexName) throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        AcknowledgedResponse deleteIndexResponse = client.indices().delete(request, RequestOptions.DEFAULT);
        if (!deleteIndexResponse.isAcknowledged()) {
            String message = "elasticsearch delete index fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void search(String indexName) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("author", "Marker"));
        sourceBuilder.from(0);
        sourceBuilder.size(10);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        int length = searchResponse.getHits().getHits().length;
        if (!(length > 0)) {
            String message = "elasticsearch search data fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }
}
