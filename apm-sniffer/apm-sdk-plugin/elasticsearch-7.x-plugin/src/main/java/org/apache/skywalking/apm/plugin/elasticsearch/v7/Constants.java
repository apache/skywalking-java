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

    //witnessClasses
    public static final String TASK_TRANSPORT_CHANNEL_WITNESS_CLASSES = "org.elasticsearch.transport.TaskTransportChannel";
    public static final String SEARCH_HITS_WITNESS_CLASSES = "org.elasticsearch.search.SearchHits";
    public static final String DB_TYPE = "Elasticsearch";

    public static final String BASE_FUTURE_METHOD = "actionGet";

    //tags
    public static final AbstractTag<String> ES_TOOK_MILLIS = Tags.ofKey("es.took_millis");
    public static final AbstractTag<String> ES_TOTAL_HITS = Tags.ofKey("es.total_hits");
    public static final AbstractTag<String> ES_INGEST_TOOK_MILLIS = Tags.ofKey("es.ingest_took_millis");
}
