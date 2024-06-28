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

package org.apache.skywalking.apm.plugin.solon;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

import java.util.List;

public class SolonPluginConfig {
    public static class Plugin {
        @PluginConfig(root = SolonPluginConfig.class)
        public static class Solon {
            /**
             * Define the max length of collected HTTP parameters. The default value(=0) means not collecting.
             */
            public static int HTTP_PARAMS_LENGTH_THRESHOLD = 0;
            /**
             * Define the max length of collected HTTP header. The default value(=0) means not collecting.
             */
            public static int HTTP_HEADERS_LENGTH_THRESHOLD = 0;
            /**
             * Define the max length of collected HTTP body. The default value(=0) means not collecting.
             */
            public static int HTTP_BODY_LENGTH_THRESHOLD = 0;
            /**
             * intercept class name, default is org.noear.solon.core.mvc.ActionDefault
             */
            public static String INTERCEPT_CLASS_NAME = "org.noear.solon.core.mvc.ActionDefault";
            /**
             * intercept method name, default is invoke
             */
            public static String INTERCEPT_METHOD_NAME = "invoke";
            /**
             * is after exception handling, default is false, if true, the plugin will intercept the method after the exception handling
             */
            public static boolean AFTER_EXCEPTION_HANDLING = false;
            /**
             * It controls what header data should be collected, values must be in lower case, if empty, collect all. default is empty.
             */
            public static List<String> INCLUDE_HTTP_HEADERS ;
        }
    }
}
