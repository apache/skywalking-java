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

package org.apache.skywalking.apm.plugin.neo4j.v4x;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class Neo4jPluginConfig {

    public static class Plugin {

        @PluginConfig(root = Neo4jPluginConfig.class)
        public static class Neo4j {

            /**
             * If set to true, the parameters of the cypher would be collected.
             */
            public static boolean TRACE_CYPHER_PARAMETERS = false;
            /**
             * For the sake of performance, SkyWalking won't save the entire parameters string into the tag, but only
             * the first {@code CYPHER_PARAMETERS_MAX_LENGTH} characters.
             * <p>
             * Set a negative number to save the complete parameter string to the tag.
             */
            public static int CYPHER_PARAMETERS_MAX_LENGTH = 512;
            /**
             * For the sake of performance, SkyWalking won't save the entire sql body into the tag, but only the first
             * {@code CYPHER_BODY_MAX_LENGTH} characters.
             * <p>
             * Set a negative number to save the complete sql body to the tag.
             */
            public static int CYPHER_BODY_MAX_LENGTH = 2048;
        }
    }
}
