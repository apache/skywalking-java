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

package org.apache.skywalking.apm.plugin.mongodb.v3.support;

import com.mongodb.MongoNamespace;
import lombok.SneakyThrows;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.mongodb.v3.MongoPluginConfig;
import org.apache.skywalking.apm.util.StringUtil;

import java.lang.reflect.Field;

public class MongoSpanHelper {

    private static final AbstractTag<String> DB_COLLECTION_TAG = Tags.ofKey("db.collection");

    private MongoSpanHelper() {
    }

    @SneakyThrows
    public static void createExitSpan(String executeMethod, String remotePeer, Object operation) {
        AbstractSpan span = ContextManager.createExitSpan(
                MongoConstants.MONGO_DB_OP_PREFIX + executeMethod, new ContextCarrier(), remotePeer);
        span.setComponent(ComponentsDefine.MONGO_DRIVER);
        Tags.DB_TYPE.set(span, MongoConstants.DB_TYPE);
        SpanLayer.asDB(span);

        Field[] declaredFields = operation.getClass().getDeclaredFields();
        MongoNamespace namespace = tryToGetMongoNamespace(operation, declaredFields);
        if (namespace != null) {
            extractTagsFromNamespace(span, namespace);
        }

        if (MongoPluginConfig.Plugin.MongoDB.TRACE_PARAM) {
            Tags.DB_BIND_VARIABLES.set(span, MongoOperationHelper.getTraceParam(operation));
        }
    }

    private static void extractTagsFromNamespace(AbstractSpan span, MongoNamespace namespace) {
        Tags.DB_INSTANCE.set(span, namespace.getDatabaseName());
        if (StringUtil.isNotEmpty(namespace.getCollectionName())) {
            span.tag(DB_COLLECTION_TAG, namespace.getCollectionName());
        }
    }

    private static MongoNamespace tryToGetMongoNamespace(Object operation, Field[] declaredFields) throws IllegalAccessException {
        Field namespaceField = null;
        Field wrappedField = null;
        Field databaseField = null;
        Field collectionField = null;
        for (Field field : declaredFields) {
            if ("namespace".equals(field.getName())) {
                namespaceField = field;
                Field.setAccessible(new Field[]{field}, true);
            }
            if ("wrapped".equals(field.getName())) {
                wrappedField = field;
                Field.setAccessible(new Field[]{field}, true);
            }
            if ("databaseName".equals(field.getName())) {
                databaseField = field;
                Field.setAccessible(new Field[]{field}, true);
            }
            if ("collectionName".equals(field.getName())) {
                collectionField = field;
                Field.setAccessible(new Field[]{field}, true);
            }
        }
        if (namespaceField != null) {
            return (MongoNamespace) namespaceField.get(operation);
        }
        String database = null;
        String collection = null;
        if (databaseField != null) {
            database = (String) databaseField.get(operation);
        }
        if (collectionField != null) {
            collection = (String) collectionField.get(operation);
        }
        if (database != null && collection == null) {
            return new MongoNamespace(database);
        }
        if (database != null && collection != null) {
            return new MongoNamespace(database, collection);
        }
        if (wrappedField != null) {
            Object wrapped = wrappedField.get(operation);
            if (wrapped != null && wrapped != operation) {
                Field[] declaredFieldsInWrapped = wrapped.getClass().getDeclaredFields();
                return tryToGetMongoNamespace(wrapped, declaredFieldsInWrapped);
            }
        }
        return null;

    }
}
