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

package org.apache.skywalking.apm.toolkit.activation.micrometer;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.Locale;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;

class TaggingHelper {

    private static final String DB_TAG_PREFIX = "jdbc";

    private static final String HTTP_TAG_PREFIX = "http";

    private static final String RPC_TAG_PREFIX = "rpc";

    private static final String MESSAGING_TAG_PREFIX = "messaging";

    static SpanLayer toLayer(KeyValues keyValues) {
        for (KeyValue keyValue : keyValues) {
            if (keyValue.getKey().toLowerCase(Locale.ROOT).startsWith(DB_TAG_PREFIX)) {
                return SpanLayer.DB;
            } else if (keyValue.getKey().toLowerCase(Locale.ROOT).startsWith(HTTP_TAG_PREFIX)) {
                return SpanLayer.HTTP;
            } else if (keyValue.getKey().toLowerCase(Locale.ROOT).startsWith(RPC_TAG_PREFIX)) {
                return SpanLayer.RPC_FRAMEWORK;
            } else if (keyValue.getKey().toLowerCase(Locale.ROOT).startsWith(MESSAGING_TAG_PREFIX)) {
                return SpanLayer.MQ;
            }
        }
        return null;
    }
}
