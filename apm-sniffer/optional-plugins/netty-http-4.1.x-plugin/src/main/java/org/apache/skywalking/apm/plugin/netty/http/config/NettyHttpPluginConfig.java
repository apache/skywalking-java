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

package org.apache.skywalking.apm.plugin.netty.http.config;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class NettyHttpPluginConfig {

    public static class Plugin {

        @PluginConfig(root = NettyHttpPluginConfig.class)
        public static class NettyHttp {

            /**
             * This config item controls that whether the Netty Http plugin should collect the http body of the request.
             */
            public static boolean COLLECT_REQUEST_BODY = false;

            /**
             * If {@link Plugin.NettyHttp#COLLECT_REQUEST_BODY} is enabled, 
             * this limitation threshold controls the max size of collected HTTP body.
             * Use the negative value to represent the unlimited threshold, which could cause OOM.
             */
            public static int FILTER_LENGTH_LIMIT = 1024;

            /**
             * If {@link Plugin.NettyHttp#COLLECT_REQUEST_BODY} is enabled, 
             * this config scopes the types of HTTP content for collecting HTTP request body.
             * Use a comma to separate multiple types.
             */
            public static String SUPPORTED_CONTENT_TYPES_PREFIX = "application/json,text/";
        }
    }
}
