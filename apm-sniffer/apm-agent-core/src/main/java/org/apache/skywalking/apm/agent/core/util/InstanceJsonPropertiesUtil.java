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

package org.apache.skywalking.apm.agent.core.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.util.StringUtil;

public class InstanceJsonPropertiesUtil {

    private static final ILog LOGGER = LogManager.getLogger(InstanceJsonPropertiesUtil.class);
    private static final String GIT_PROPERTIES = "skywalking-agent-git.properties";
    private static final Gson GSON = new Gson();

    public static List<KeyStringValuePair> parseProperties() {
        List<KeyStringValuePair> properties = new ArrayList<>();

        if (StringUtil.isNotEmpty(Config.Agent.INSTANCE_PROPERTIES_JSON)) {
            Map<String, String> json = GSON.fromJson(
                Config.Agent.INSTANCE_PROPERTIES_JSON,
                new TypeToken<Map<String, String>>() {
                }.getType()
            );
            json.forEach(
                (key, val) -> properties.add(KeyStringValuePair.newBuilder().setKey(key).setValue(val).build()));
        }

        properties.add(KeyStringValuePair.newBuilder().setKey("namespace").setValue(Config.Agent.NAMESPACE).build());
        properties.add(KeyStringValuePair.newBuilder().setKey("cluster").setValue(Config.Agent.CLUSTER).build());
        properties.add(KeyStringValuePair.newBuilder().setKey("version").setValue(getAgentVersion()).build());

        return properties;
    }

    public static String getAgentVersion() {
        try {
            InputStream inStream = InstanceJsonPropertiesUtil.class.getClassLoader()
                    .getResourceAsStream(GIT_PROPERTIES);
            if (inStream != null) {
                Properties gitProperties = new Properties();
                gitProperties.load(inStream);
                String commitIdAbbrev = gitProperties.getProperty("git.commit.id.abbrev");
                String buildTime = gitProperties.getProperty("git.build.time");
                String buildVersion = gitProperties.getProperty("git.build.version");
                String version =  buildVersion + "-" + commitIdAbbrev + "-" + buildTime;

                LOGGER.info("SkyWalking agent version: {}", version);
                return version;
            } else {
                LOGGER.warn("{} not found, SkyWalking agent version: unknown", GIT_PROPERTIES);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get agent version", e);
        }

        return "UNKNOWN";
    }
}
