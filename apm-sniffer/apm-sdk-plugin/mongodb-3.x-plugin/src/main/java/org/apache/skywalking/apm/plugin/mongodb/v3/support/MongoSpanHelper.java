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
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.mongodb.v3.MongoPluginConfig;

import java.lang.reflect.Field;

public class MongoSpanHelper {

    private MongoSpanHelper() {
    }

    public static void createExitSpan(String executeMethod, String remotePeer, Object operation) {
        AbstractSpan span = ContextManager.createExitSpan(
            MongoConstants.MONGO_DB_OP_PREFIX + executeMethod, new ContextCarrier(), remotePeer);
        span.setComponent(ComponentsDefine.MONGO_DRIVER);
        Tags.DB_TYPE.set(span, MongoConstants.DB_TYPE);
        SpanLayer.asDB(span);

        try {
            Field namespaceField = operation.getClass().getDeclaredField("namespace");
            Field.setAccessible(new Field[]{namespaceField}, true);
            MongoNamespace namespace = (MongoNamespace) namespaceField.get(operation);
            Tags.DB_INSTANCE.set(span, namespace.getFullName());
        } catch (Exception e) {
            try {
                Field wrappedField = operation.getClass().getDeclaredField("wrapped");
                Field.setAccessible(new Field[]{wrappedField}, true);
                Object wrappedOperation = wrappedField.get(operation);
                Field wrappedNamespaceField = wrappedOperation.getClass().getDeclaredField("namespace");
                Field.setAccessible(new Field[]{wrappedNamespaceField}, true);
                MongoNamespace wrappedNamespace = (MongoNamespace) wrappedNamespaceField.get(wrappedOperation);
                Tags.DB_INSTANCE.set(span, wrappedNamespace.getFullName());
            } catch (Exception e2) {

            }
        }

        if (operation instanceof EnhancedInstance) {
            Object databaseName = ((EnhancedInstance) operation).getSkyWalkingDynamicField();
            if (databaseName != null) {
                Tags.DB_INSTANCE.set(span, (String) databaseName);
            }
        }

        if (MongoPluginConfig.Plugin.MongoDB.TRACE_PARAM) {
            Tags.DB_BIND_VARIABLES.set(span, MongoOperationHelper.getTraceParam(operation));
        }
    }
}
