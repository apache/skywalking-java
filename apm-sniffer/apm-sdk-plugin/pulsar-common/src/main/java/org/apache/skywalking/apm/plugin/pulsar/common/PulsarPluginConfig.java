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

package org.apache.skywalking.apm.plugin.pulsar.common;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class PulsarPluginConfig {

    public static class Plugin {
        @PluginConfig(root = PulsarPluginConfig.class)
        public static class Pulsar {
            /**
             * If set to true, the message contents of the Pulsar topic would be collected.
             */
            public static boolean TRACE_MESSAGE_CONTENTS = false;
            /**
             * For the sake of performance, SkyWalking won't save message contents string into the tag.
             * If TRACE_MESSAGE_CONTENTS is set to true, the first {@code MESSAGE_CONTENTS_MAX_LENGTH} parameter
             * characters would be collected.
             * <p>
             * Set a negative number to save specified length of content string to the tag.
             */
            public static int MESSAGE_CONTENTS_MAX_LENGTH = 256;

        }
    }
}
