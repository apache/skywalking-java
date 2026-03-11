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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class HttpClient5PluginConfig {
    public static class Plugin {
        @PluginConfig(root = HttpClient5PluginConfig.class)
        public static class HttpClient5 {
            /**
             * Comma-separated list of destination ports whose outbound HTTP requests
             * will be completely skipped by the classic client interceptor: no exit
             * span is created and no SkyWalking propagation headers are injected.
             *
             * <p>Some HTTP-based database protocols (e.g. ClickHouse on port 8123)
             * reject requests that contain unknown HTTP headers, returning HTTP 400.
             * Adding such ports here prevents the agent from creating exit spans
             * and from injecting the {@code sw8} tracing headers into those outbound
             * requests, meaning these requests are completely untraced by SkyWalking,
             * while leaving all other HTTP calls fully traced.
             *
             * <p>Default: {@code "8123"} (ClickHouse HTTP interface).
             *
             * <p>Example – also exclude port 9200 (Elasticsearch):
             * {@code plugin.httpclient5.PROPAGATION_EXCLUDE_PORTS=8123,9200}
             */
            public static String PROPAGATION_EXCLUDE_PORTS = "8123";
        }
    }
}
