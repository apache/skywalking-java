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

package org.apache.skywalking.apm.plugin.elasticsearch.v7;

import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;

public class Constants {

    public static final String INDICES_CLIENT_CREATE_METHODS_INTERCEPTOR = "org.apache.skywalking.apm.plugin.elasticsearch.v7.interceptor.IndicesClientCreateMethodsInterceptor";
    public static final String INDICES_CLIENT_DELETE_METHODS_INTERCEPTOR = "org.apache.skywalking.apm.plugin.elasticsearch.v7.interceptor.IndicesClientDeleteMethodsInterceptor";
    public static final String INDICES_CLIENT_ANALYZE_METHODS_INTERCEPTOR = "org.apache.skywalking.apm.plugin.elasticsearch.v7.interceptor.IndicesClientAnalyzeMethodsInterceptor";
    public static final String INDICES_CLIENT_REFRESH_METHODS_INTERCEPTOR = "org.apache.skywalking.apm.plugin.elasticsearch.v7.interceptor.IndicesClientRefreshMethodsInterceptor";

    //witnessClasses
    public static final String TASK_TRANSPORT_CHANNEL_WITNESS_CLASSES = "org.elasticsearch.transport.TaskTransportChannel";
    public static final String SEARCH_HITS_WITNESS_CLASSES = "org.elasticsearch.search.SearchHits";
    public static final String CREATE_INDEX_REQUEST_WITNESS_CLASS = "org.elasticsearch.client.indices.CreateIndexRequest";
    public static final String DELETE_INDEX_REQUEST_WITNESS_CLASS = "org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest";
    public static final String ANALYZE_REQUEST_WITNESS_CLASS = "org.elasticsearch.client.indices.AnalyzeRequest";
    public static final String REFRESH_REQUEST_WITNESS_CLASS = "org.elasticsearch.action.admin.indices.refresh.RefreshRequest";
    public static final String DB_TYPE = "Elasticsearch";

    public static final String BASE_FUTURE_METHOD = "actionGet";

    public static final String CREATE_OPERATOR_NAME = "Elasticsearch/CreateRequest";
    public static final String DELETE_OPERATOR_NAME = "Elasticsearch/DeleteRequest";
    public static final String ANALYZE_OPERATOR_NAME = "Elasticsearch/AnalyzeRequest";
    public static final String REFRESH_OPERATOR_NAME = "Elasticsearch/RefreshRequest";

    //tags
    public static final AbstractTag<String> ES_TOOK_MILLIS = Tags.ofKey("es.took_millis");
    public static final AbstractTag<String> ES_TOTAL_HITS = Tags.ofKey("es.total_hits");
    public static final AbstractTag<String> ES_INGEST_TOOK_MILLIS = Tags.ofKey("es.ingest_took_millis");

    public static final String ON_RESPONSE_SUFFIX = "/onResponse";
    public static final String ON_FAILURE_SUFFIX = "/onFailure";

}
