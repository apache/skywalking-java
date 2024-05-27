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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.skywalking.apm.testcase.elasticsearch.controller.CaseController;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
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
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
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
        request.timeout("10s");
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
        String indexName2 = UUID.randomUUID().toString();
        try {
            // health
            health();

            // get settings
            getSettings();

            // put settings
            putSettings();

            // create
            createIndex(indexName);
            createIndexAsync(indexName2);

            // index
            index(indexName);
            indexAsync(indexName2);

            // refresh
            client.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
            client.indices().refresh(new RefreshRequest(indexName2), RequestOptions.DEFAULT);

            // get
            get(indexName);
            getAsync(indexName2);

            // search
            search(indexName);
            searchAsync(indexName2);

            // update
            update(indexName);
            updateAsync(indexName2);

            // analyze
            analyze(indexName);
            analyzeAsync(indexName2);

            // delete
            delete(indexName);
            deleteAsync(indexName2);
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
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> mappingProperties = new HashMap<>();
        Map<String, String> mappingPropertiesAuthor = new HashMap<>();
        mappingPropertiesAuthor.put("type", "keyword");
        mappingProperties.put("author", mappingPropertiesAuthor);
        Map<String, String> mappingPropertiesTitle = new HashMap<>();
        mappingPropertiesTitle.put("type", "keyword");
        mappingProperties.put("title", mappingPropertiesTitle);
        mapping.put("properties", mappingProperties);
        request.mapping(mapping);

        request.settings(Settings.builder().put("index.number_of_shards", 1).put("index.number_of_replicas", 0));

        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
        if (!createIndexResponse.isAcknowledged()) {
            String message = "elasticsearch create index fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void createIndexAsync(String indexName) throws InterruptedException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> mappingProperties = new HashMap<>();
        Map<String, String> mappingPropertiesAuthor = new HashMap<>();
        mappingPropertiesAuthor.put("type", "keyword");
        mappingProperties.put("author", mappingPropertiesAuthor);
        Map<String, String> mappingPropertiesTitle = new HashMap<>();
        mappingPropertiesTitle.put("type", "keyword");
        mappingProperties.put("title", mappingPropertiesTitle);
        mapping.put("properties", mappingProperties);
        request.mapping(mapping);

        request.settings(Settings.builder().put("index.number_of_shards", 1).put("index.number_of_replicas", 0));

        final CountDownLatch latch = new CountDownLatch(1);
        client.indices().createAsync(request, RequestOptions.DEFAULT, new ActionListener<CreateIndexResponse>() {
            @Override
            public void onResponse(final CreateIndexResponse createIndexResponse) {
                latch.countDown();
                if (!createIndexResponse.isAcknowledged()) {
                    String message = "elasticsearch create index fail.";
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
            }

            @Override
            public void onFailure(final Exception e) {
                latch.countDown();
            }
        });
        latch.await();
    }

    private void index(String indexName) throws IOException {
        Map<String, String> source = new HashMap<>();
        source.put("author", "Marker");
        source.put("title", "Java programing.");
        IndexRequest indexRequest = new IndexRequest(indexName).id("1").source(source);

        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        if (indexResponse.status().getStatus() >= 400) {
            String message = "elasticsearch index data fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
        client.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
    }

    private void indexAsync(String indexName) throws InterruptedException {
        Map<String, String> source = new HashMap<>();
        source.put("author", "Marker");
        source.put("title", "Java programing.");
        IndexRequest indexRequest = new IndexRequest(indexName).id("1").source(source);

        final CountDownLatch latch = new CountDownLatch(2);
        client.indexAsync(indexRequest, RequestOptions.DEFAULT,
                          new ActionListener<IndexResponse>() {
                              @Override
                              public void onResponse(final IndexResponse indexResponse) {
                                  latch.countDown();
                                  if (indexResponse.status().getStatus() >= 400) {
                                      String message = "elasticsearch index data fail.";
                                      LOGGER.error(message);
                                      throw new RuntimeException(message);
                                  }
                              }

                              @Override
                              public void onFailure(final Exception e) {
                                  latch.countDown();
                              }
                          }
        );

        client.indices().refreshAsync(new RefreshRequest(indexName), RequestOptions.DEFAULT,
                                      new ActionListener<RefreshResponse>() {
                                          @Override
                                          public void onResponse(final RefreshResponse refreshResponse) {
                                              latch.countDown();
                                          }

                                          @Override
                                          public void onFailure(final Exception e) {
                                              latch.countDown();
                                          }
                                      }
        );
        latch.await();
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

    private void getAsync(String indexName) throws InterruptedException {
        GetRequest getRequest = new GetRequest(indexName, "1");
        final CountDownLatch latch = new CountDownLatch(1);
        client.getAsync(getRequest, RequestOptions.DEFAULT,
                        new ActionListener<GetResponse>() {
                            @Override
                            public void onResponse(final GetResponse getResponse) {
                                latch.countDown();
                                if (!getResponse.isExists()) {
                                    String message = "elasticsearch get data fail.";
                                    LOGGER.error(message);
                                    throw new RuntimeException(message);
                                }
                            }

                            @Override
                            public void onFailure(final Exception e) {
                                latch.countDown();
                                throw new RuntimeException(e);
                            }
                        }
        );
       latch.await();
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

    private void updateAsync(String indexName) throws InterruptedException {
        UpdateRequest request = new UpdateRequest(indexName, "1");
        Map<String, Object> parameters = singletonMap("title", "c++ programing.");
        Script inline = new Script(ScriptType.INLINE, "painless", "ctx._source.title = params.title", parameters);
        request.script(inline);

        final CountDownLatch latch = new CountDownLatch(1);
        client.updateAsync(request, RequestOptions.DEFAULT, new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(final UpdateResponse updateResponse) {
                latch.countDown();
                if (updateResponse.getVersion() != 2) {
                    String message = "elasticsearch update data fail.";
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
            }

            @Override
            public void onFailure(final Exception e) {
                latch.countDown();
                throw new RuntimeException(e);
            }
        });
        latch.await();
    }

    private void analyze(String indexName) throws IOException {
        AnalyzeRequest analyzeRequest = AnalyzeRequest.withIndexAnalyzer(indexName, null, "SkyWalking");
        AnalyzeResponse analyzeResponse = client.indices().analyze(analyzeRequest, RequestOptions.DEFAULT);
        if (null == analyzeResponse.getTokens() || analyzeResponse.getTokens().size() < 1) {
            String message = "elasticsearch analyze index fail.";
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

    private void analyzeAsync(String indexName) throws InterruptedException {
        AnalyzeRequest analyzeRequest = AnalyzeRequest.withIndexAnalyzer(indexName, null, "SkyWalking");
        final CountDownLatch latch = new CountDownLatch(1);
        client.indices().analyzeAsync(
            analyzeRequest, RequestOptions.DEFAULT, new ActionListener<AnalyzeResponse>() {
                @Override
                public void onResponse(final AnalyzeResponse analyzeResponse) {
                    latch.countDown();
                    if (null == analyzeResponse.getTokens() || analyzeResponse.getTokens().size() < 1) {
                        String message = "elasticsearch analyze index fail.";
                        LOGGER.error(message);
                        throw new RuntimeException(message);
                    }
                }

                @Override
                public void onFailure(final Exception e) {
                    latch.countDown();
                    LOGGER.error(e);
                }
            });
        latch.await();
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

    private void deleteAsync(String indexName) throws InterruptedException {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        final CountDownLatch latch = new CountDownLatch(1);
        client.indices().deleteAsync(request, RequestOptions.DEFAULT, new ActionListener<AcknowledgedResponse>() {
            @Override
            public void onResponse(final AcknowledgedResponse acknowledgedResponse) {
                latch.countDown();
                if (!acknowledgedResponse.isAcknowledged()) {
                    String message = "elasticsearch delete index fail.";
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
            }

            @Override
            public void onFailure(final Exception e) {
                latch.countDown();
                throw new RuntimeException(e);
            }
        });
        latch.await();
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

    private void searchAsync(String indexName) throws InterruptedException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("author", "Marker"));
        sourceBuilder.from(0);
        sourceBuilder.size(10);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        searchRequest.source(sourceBuilder);
        final CountDownLatch latch = new CountDownLatch(1);
        client.searchAsync(searchRequest, RequestOptions.DEFAULT, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(final SearchResponse searchResponse) {
                latch.countDown();
                int length = searchResponse.getHits().getHits().length;
                if (!(length > 0)) {
                    String message = "elasticsearch search data fail.";
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
            }

            @Override
            public void onFailure(final Exception e) {
                latch.countDown();
                LOGGER.error(e);
            }
        });
        latch.await();
    }
}
