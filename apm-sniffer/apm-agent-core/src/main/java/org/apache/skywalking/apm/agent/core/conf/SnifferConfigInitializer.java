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

package org.apache.skywalking.apm.agent.core.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.logging.core.JsonLogResolver;
import org.apache.skywalking.apm.agent.core.logging.core.PatternLogResolver;
import org.apache.skywalking.apm.util.ConfigInitializer;
import org.apache.skywalking.apm.util.PropertyPlaceholderHelper;
import org.apache.skywalking.apm.util.StringUtil;

import static org.apache.skywalking.apm.agent.core.conf.Constants.SERVICE_NAME_PART_CONNECTOR;

/**
 * The <code>SnifferConfigInitializer</code> initializes all configs in several way.
 */
public class SnifferConfigInitializer {
    private static ILog LOGGER = LogManager.getLogger(SnifferConfigInitializer.class);
    private static final String SPECIFIED_CONFIG_PATH = "skywalking_config";
    private static final String DEFAULT_CONFIG_FILE_NAME = "/config/agent.config";
    private static final String ENV_KEY_PREFIX = "skywalking.";
    private static Properties AGENT_SETTINGS;
    private static boolean IS_INIT_COMPLETED = false;

    /**
     * If the specified agent config path is set, the agent will try to locate the specified agent config. If the
     * specified agent config path is not set , the agent will try to locate `agent.config`, which should be in the
     * /config directory of agent package.
     * <p>
     * Also try to override the config by system.properties. All the keys in this place should start with {@link
     * #ENV_KEY_PREFIX}. e.g. in env `skywalking.agent.service_name=yourAppName` to override `agent.service_name` in
     * config file.
     * <p>
     * At the end, `agent.service_name` and `collector.servers` must not be blank.
     */
    public static void initializeCoreConfig(String agentOptions) {
        AGENT_SETTINGS = new Properties();
        try (final InputStreamReader configFileStream = loadConfig()) {
            AGENT_SETTINGS.load(configFileStream);
            for (String key : AGENT_SETTINGS.stringPropertyNames()) {
                String value = (String) AGENT_SETTINGS.get(key);
                AGENT_SETTINGS.put(key, PropertyPlaceholderHelper.INSTANCE.replacePlaceholders(value, AGENT_SETTINGS));
            }

        } catch (Exception e) {
            LOGGER.error(e, "Failed to read the config file, skywalking is going to run in default config.");
        }

        try {
            overrideConfigBySystemProp();
        } catch (Exception e) {
            LOGGER.error(e, "Failed to read the system properties.");
        }

        agentOptions = StringUtil.trim(agentOptions, ',');
        if (!StringUtil.isEmpty(agentOptions)) {
            try {
                agentOptions = agentOptions.trim();
                LOGGER.info("Agent options is {}.", agentOptions);

                overrideConfigByAgentOptions(agentOptions);
            } catch (Exception e) {
                LOGGER.error(e, "Failed to parse the agent options, val is {}.", agentOptions);
            }
        }

        initializeConfig(Config.class);
        // reconfigure logger after config initialization
        configureLogger();
        LOGGER = LogManager.getLogger(SnifferConfigInitializer.class);

        setAgentVersion();

        if (StringUtil.isEmpty(Config.Agent.SERVICE_NAME)) {
            throw new ExceptionInInitializerError("`agent.service_name` is missing.");
        } else {
            if (StringUtil.isNotEmpty(Config.Agent.NAMESPACE) || StringUtil.isNotEmpty(Config.Agent.CLUSTER)) {
                Config.Agent.SERVICE_NAME = StringUtil.join(
                    SERVICE_NAME_PART_CONNECTOR,
                    Config.Agent.SERVICE_NAME,
                    Config.Agent.NAMESPACE,
                    Config.Agent.CLUSTER
                );
            }
        }
        if (StringUtil.isEmpty(Config.Collector.BACKEND_SERVICE)) {
            throw new ExceptionInInitializerError("`collector.backend_service` is missing.");
        }
        if (Config.Plugin.PEER_MAX_LENGTH <= 3) {
            LOGGER.warn(
                "PEER_MAX_LENGTH configuration:{} error, the default value of 200 will be used.",
                Config.Plugin.PEER_MAX_LENGTH
            );
            Config.Plugin.PEER_MAX_LENGTH = 200;
        }

        IS_INIT_COMPLETED = true;
    }

    /**
     * Initialize field values of any given config class.
     *
     * @param configClass to host the settings for code access.
     */
    public static void initializeConfig(Class configClass) {
        if (AGENT_SETTINGS == null) {
            LOGGER.error("Plugin configs have to be initialized after core config initialization.");
            return;
        }
        try {
            ConfigInitializer.initialize(AGENT_SETTINGS, configClass);
        } catch (IllegalAccessException e) {
            LOGGER.error(e,
                         "Failed to set the agent settings {}"
                             + " to Config={} ",
                         AGENT_SETTINGS, configClass
            );
        }
    }

    private static void overrideConfigByAgentOptions(String agentOptions) throws IllegalArgumentException {
        for (List<String> terms : parseAgentOptions(agentOptions)) {
            if (terms.size() != 2) {
                throw new IllegalArgumentException("[" + terms + "] is not a key-value pair.");
            }
            AGENT_SETTINGS.put(terms.get(0), terms.get(1));
        }
    }

    private static List<List<String>> parseAgentOptions(String agentOptions) {
        List<List<String>> options = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        boolean isInQuotes = false;
        StringBuilder currentTerm = new StringBuilder();
        for (char c : agentOptions.toCharArray()) {
            if (c == '\'' || c == '"') {
                isInQuotes = !isInQuotes;
            } else if (c == '=' && !isInQuotes) {   // key-value pair uses '=' as separator
                terms.add(currentTerm.toString());
                currentTerm = new StringBuilder();
            } else if (c == ',' && !isInQuotes) {   // multiple options use ',' as separator
                terms.add(currentTerm.toString());
                currentTerm = new StringBuilder();

                options.add(terms);
                terms = new ArrayList<>();
            } else {
                currentTerm.append(c);
            }
        }
        // add the last term and option without separator
        terms.add(currentTerm.toString());
        options.add(terms);
        return options;
    }

    public static boolean isInitCompleted() {
        return IS_INIT_COMPLETED;
    }

    /**
     * Override the config by system properties. The property key must start with `skywalking`, the result should be as
     * same as in `agent.config`
     * <p>
     * such as: Property key of `agent.service_name` should be `skywalking.agent.service_name`
     */
    private static void overrideConfigBySystemProp() {
        Properties systemProperties = System.getProperties();
        for (final Map.Entry<Object, Object> prop : systemProperties.entrySet()) {
            String key = prop.getKey().toString();
            if (key.startsWith(ENV_KEY_PREFIX)) {
                String realKey = key.substring(ENV_KEY_PREFIX.length());
                AGENT_SETTINGS.put(realKey, prop.getValue());
            }
        }
    }

    /**
     * Set agent version(Described in MANIFEST.MF)
     */
    private static void setAgentVersion() {
        try {
            Enumeration<URL> resources = SnifferConfigInitializer.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                LOGGER.info("SnifferConfigInitializer url:{}", url.toString());
                try (InputStream is = url.openStream()) {
                    if (is != null) {
                        Manifest manifest = new Manifest(is);
                        Attributes mainAttribs = manifest.getMainAttributes();
                        String projectName = mainAttribs.getValue("Implementation-Vendor-Id");
                        if (projectName != null) {
                            if ("org.apache.skywalking".equals(projectName)) {
                                Config.Agent.VERSION = mainAttribs.getValue("Implementation-Version");
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Can't read version from MANIFEST.MF in the agent jar");
        }
    }

    /**
     * Load the specified config file or default config file
     *
     * @return the config file {@link InputStream}, or null if not needEnhance.
     */
    private static InputStreamReader loadConfig() throws AgentPackageNotFoundException, ConfigNotFoundException {
        String specifiedConfigPath = System.getProperty(SPECIFIED_CONFIG_PATH);
        File configFile = StringUtil.isEmpty(specifiedConfigPath) ? new File(
            AgentPackagePath.getPath(), DEFAULT_CONFIG_FILE_NAME) : new File(specifiedConfigPath);

        if (configFile.exists() && configFile.isFile()) {
            try {
                LOGGER.info("Config file found in {}.", configFile);

                return new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                throw new ConfigNotFoundException("Failed to load agent.config", e);
            }
        }
        throw new ConfigNotFoundException("Failed to load agent.config.");
    }

    static void configureLogger() {
        switch (Config.Logging.RESOLVER) {
            case JSON:
                LogManager.setLogResolver(new JsonLogResolver());
                break;
            case PATTERN:
            default:
                LogManager.setLogResolver(new PatternLogResolver());
        }
    }
}
