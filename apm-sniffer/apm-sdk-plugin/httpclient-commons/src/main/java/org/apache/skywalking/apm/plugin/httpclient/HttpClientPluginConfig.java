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

package org.apache.skywalking.apm.plugin.httpclient;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class HttpClientPluginConfig {
    public static class Plugin {
        @PluginConfig(root = HttpClientPluginConfig.class)
        public static class HttpClient {
            /**
             * This config item controls that whether the HttpClient plugin should collect the parameters of the request.
             */
            public static boolean COLLECT_HTTP_PARAMS = false;

            /**
             * Comma-separated list of destination ports whose outbound HTTP requests
             * will be completely skipped by the httpclient-4.x interceptor: no exit
             * span is created and no SkyWalking propagation headers are injected.
             *
             * <p>Some HTTP-based database protocols (e.g. ClickHouse on port 8123)
             * reject requests that contain unknown HTTP headers, returning HTTP 400.
             * Adding such ports here prevents the agent from creating exit spans
             * and from injecting the {@code sw8} tracing headers into those outbound
             * requests.
             *
             * <p>Default: {@code "8123"} (ClickHouse HTTP interface).
             *
             * <p>Example – also exclude port 9200 (Elasticsearch):
             * {@code plugin.httpclient.propagation_exclude_ports=8123,9200}
             */
            public static String PROPAGATION_EXCLUDE_PORTS = "8123";
        }

        @PluginConfig(root = HttpClientPluginConfig.class)
        public static class Http {
            /**
             * When either {@link HttpClient#COLLECT_HTTP_PARAMS} is enabled, how many characters to keep and send to the
             * OAP backend, use negative values to keep and send the complete parameters, NB. this config item is added
             * for the sake of performance
             */
            public static int HTTP_PARAMS_LENGTH_THRESHOLD = 1024;
        }
    }
}
