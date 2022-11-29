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

package org.apache.skywalking.apm.agent.core.version;

import lombok.Getter;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public enum Version {
    CURRENT;

    private static final ILog LOGGER = LogManager.getLogger(Version.class);
    private static final String VERSION_FILE_NAME = "skywalking-agent-version.properties";
    private final String buildVersion;
    private final String commitIdAbbrev;

    Version() {
        try {
            InputStream inputStream = Version.class.getClassLoader().getResourceAsStream(VERSION_FILE_NAME);
            if (inputStream == null) {
                throw new IOException("Can't find " + VERSION_FILE_NAME);
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            buildVersion = properties.getProperty("git.build.version");
            commitIdAbbrev = properties.getProperty("git.commit.id.abbrev");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static {
        LOGGER.info("SkyWalking agent version: {}", CURRENT);
    }

    @Override
    public String toString() {
        return String.format("%s-%s", buildVersion, commitIdAbbrev);
    }
}
