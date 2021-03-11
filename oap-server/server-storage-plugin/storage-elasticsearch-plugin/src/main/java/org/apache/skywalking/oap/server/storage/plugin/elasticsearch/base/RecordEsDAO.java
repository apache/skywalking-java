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

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base;

import java.io.IOException;
import org.apache.skywalking.oap.server.core.analysis.record.Record;
import org.apache.skywalking.oap.server.core.storage.IRecordDAO;
import org.apache.skywalking.oap.server.core.storage.StorageHashMapBuilder;
import org.apache.skywalking.oap.server.core.storage.model.Model;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.library.client.request.InsertRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class RecordEsDAO extends EsDAO implements IRecordDAO {
    private final StorageHashMapBuilder<Record> storageBuilder;

    public RecordEsDAO(ElasticSearchClient client,
                       StorageHashMapBuilder<Record> storageBuilder) {
        super(client);
        this.storageBuilder = storageBuilder;
    }

    @Override
    public InsertRequest prepareBatchInsert(Model model, Record record) throws IOException {
        XContentBuilder builder = map2builder(
            IndexController.INSTANCE.appendMetricTableColumn(model, storageBuilder.entity2Storage(record)));
        String modelName = TimeSeriesUtils.writeIndexName(model, record.getTimeBucket());
        String id = IndexController.INSTANCE.generateDocId(model, record.id());
        return getClient().prepareInsert(modelName, id, builder);
    }
}
