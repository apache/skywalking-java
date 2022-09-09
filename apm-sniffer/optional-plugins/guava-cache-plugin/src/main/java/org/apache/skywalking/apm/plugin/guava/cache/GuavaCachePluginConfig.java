/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.guava.cache;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GuavaCachePluginConfig {
    public static class Plugin {
        @PluginConfig(root = GuavaCachePluginConfig.class)
        public static class GuavaCache {
            /**
             * Operation represent a cache span is "write" or "read" action , and "op"(operation) is tagged with key "cache.op" usually
             * This config term define which command should be converted to write Operation .
             *
             * @see org.apache.skywalking.apm.agent.core.context.tag.Tags#CACHE_OP
             * @see GuavaCacheOperationConvertor#parseOperation(String)
             */
            public static Set<String> OPERATION_MAPPING_WRITE = new HashSet<>(Arrays.asList(
                    "put",
                    "putAll",
                    "invalidate",
                    "invalidateAll",
                    "invalidateAll",
                    "cleanUp"
            ));
            /**
             * Operation represent a cache span is "write" or "read" action , and "op"(operation) is tagged with key "cache.op" usually
             * This config term define which command should be converted to write Operation .
             *
             * @see org.apache.skywalking.apm.agent.core.context.tag.Tags#CACHE_OP
             * @see GuavaCacheOperationConvertor#parseOperation(String)
             */
            public static Set<String> OPERATION_MAPPING_READ = new HashSet<>(Arrays.asList(
                    "getIfPresent",
                    "get",
                    "getAllPresent",
                    "size"
            ));
        }
    }
}
