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

import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

public class MongoSpanHelper {

    private static final ILog LOGGER = LogManager.getLogger(MongoSpanHelper.class);

    private MongoSpanHelper() {
    }

    /**
     * createExitSpan
     * @param executeMethod executeMethod
     * @param remotePeer remotePeer
     * @param operation operation
     */
    public static void createExitSpan(String executeMethod, String remotePeer, Object operation) {
        AbstractSpan span = ContextManager.createExitSpan(
            MongoConstants.MONGO_DB_OP_PREFIX + executeMethod, new ContextCarrier(), remotePeer);
        span.setComponent(ComponentsDefine.MONGO_DRIVER);
        Tags.DB_TYPE.set(span, MongoConstants.DB_TYPE);
        SpanLayer.asDB(span);

        String databaseName = null;
        try {
            databaseName = MongoInfoHelper.getMongoDatabaseName(operation);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.warn("mongoV4 unable to obtain database name: {}",operation.getClass());
        }
        if(StringUtil.isNotEmpty(databaseName)){
            Tags.DB_INSTANCE.set(span, databaseName);
        }

        if (MongoPluginConfig.Plugin.MongoDB.TRACE_PARAM) {
            Tags.DB_BIND_VARIABLES.set(span, MongoInfoHelper.getTraceParam(operation));
        }
    }
}
