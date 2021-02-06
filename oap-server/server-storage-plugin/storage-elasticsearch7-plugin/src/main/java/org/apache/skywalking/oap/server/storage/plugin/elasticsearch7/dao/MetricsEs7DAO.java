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

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch7.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.skywalking.oap.server.core.analysis.metrics.Metrics;
import org.apache.skywalking.oap.server.core.storage.StorageHashMapBuilder;
import org.apache.skywalking.oap.server.core.storage.model.Model;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.MetricsEsDAO;
import org.elasticsearch.action.search.SearchResponse;

public class MetricsEs7DAO extends MetricsEsDAO {

    MetricsEs7DAO(final ElasticSearchClient client, final StorageHashMapBuilder<Metrics> storageBuilder) {
        super(client, storageBuilder);
    }

    @Override
    public List<Metrics> multiGet(Model model, List<Metrics> metrics) throws IOException {
        String[] ids = metrics.stream().map(Metrics::id).toArray(String[]::new);
        SearchResponse response = getClient().ids(model.getName(), ids);

        List<Metrics> result = new ArrayList<>(response.getHits().getHits().length);
        for (int i = 0; i < response.getHits().getHits().length; i++) {
            Metrics source = storageBuilder.storage2Entity(response.getHits().getAt(i).getSourceAsMap());
            result.add(source);
        }
        return result;
    }
}
