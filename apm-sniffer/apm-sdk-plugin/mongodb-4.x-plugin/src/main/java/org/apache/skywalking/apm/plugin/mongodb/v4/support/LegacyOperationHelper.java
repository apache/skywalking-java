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

package org.apache.skywalking.apm.plugin.mongodb.v4.support;

import com.mongodb.internal.bulk.DeleteRequest;
import com.mongodb.internal.bulk.InsertRequest;
import com.mongodb.internal.bulk.UpdateRequest;
import com.mongodb.internal.operation.DeleteOperation;
import com.mongodb.internal.operation.InsertOperation;
import com.mongodb.internal.operation.UpdateOperation;

import java.util.List;

/**
 * Handles trace parameter extraction for legacy MongoDB driver versions (4.0 - 4.8).
 * InsertOperation, DeleteOperation, and UpdateOperation were removed in driver 4.9.
 * This class is only loaded when those classes exist (guarded by
 * {@link MongoOperationHelper#HAS_LEGACY_WRITE_OPERATIONS}).
 */
@SuppressWarnings("deprecation")
class LegacyOperationHelper {

    private LegacyOperationHelper() {
    }

    /**
     * Extract trace parameters from legacy write operation types.
     * @return the trace parameter string, or null if obj is not a legacy write operation
     */
    static String getTraceParam(Object obj) {
        if (obj instanceof DeleteOperation) {
            List<DeleteRequest> writeRequestList = ((DeleteOperation) obj).getDeleteRequests();
            return MongoOperationHelper.getFilter(writeRequestList);
        } else if (obj instanceof InsertOperation) {
            List<InsertRequest> writeRequestList = ((InsertOperation) obj).getInsertRequests();
            return MongoOperationHelper.getFilter(writeRequestList);
        } else if (obj instanceof UpdateOperation) {
            List<UpdateRequest> writeRequestList = ((UpdateOperation) obj).getUpdateRequests();
            return MongoOperationHelper.getFilter(writeRequestList);
        }
        return null;
    }
}
