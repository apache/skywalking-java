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

package org.apache.skywalking.apm.plugin.spring.ai.v1.config;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class SpringAiPluginConfig {

    public static class Plugin {

        @PluginConfig(root = SpringAiPluginConfig.class)
        public static class SpringAi {

            /**
             * Whether to collect the prompt content (input text) of the GenAI request.
             */
            public static boolean COLLECT_INPUT_MESSAGES = false;

            /**
             * Whether to collect the completion content (output text) of the GenAI response.
             */
            public static boolean COLLECT_OUTPUT_MESSAGES = false;

            /**
             * The maximum characters of the collected prompt content.
             * If the content exceeds this limit, it will be truncated.
             * Use a negative value to represent no limit, but be aware this could cause OOM.
             */
            public static int INPUT_MESSAGES_LENGTH_LIMIT = 1024;

            /**
             * The maximum characters of the collected completion content.
             * If the content exceeds this limit, it will be truncated.
             * Use a negative value to represent no limit, but be aware this could cause OOM.
             */
            public static int OUTPUT_MESSAGES_LENGTH_LIMIT = 1024;

            /**
             * The threshold for token usage to trigger content collection.
             * When set to a positive value, prompt and completion will only be collected
             * if the total token usage of the request exceeds this threshold.
             * * This requires {@link #COLLECT_INPUT_MESSAGES} or {@link #COLLECT_OUTPUT_MESSAGES} to be enabled first.
             * Use a negative value to disable this threshold-based filtering (collect all).
             */
            public static int CONTENT_COLLECT_THRESHOLD_TOKENS = -1;

            /**
             * Whether to collect the arguments (input parameters) of the tool/function call.
             */
            public static boolean COLLECT_TOOL_INPUT = false;

            /**
             * Whether to collect the execution result (output) of the tool/function call.
             */
            public static boolean COLLECT_TOOL_OUTPUT = false;
        }
    }
}
