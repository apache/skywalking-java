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

package org.apache.skywalking.apm.agent.core.plugin.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.boot.PluginConfig;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.conf.SnifferConfigInitializer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.PluginBootstrap;

/**
 * The <code>AgentClassLoader</code> represents a classloader, which is in charge of finding plugins and interceptors.
 */
public class AgentClassLoader extends URLClassLoader {
    private static final ILog LOGGER;
    /**
     * Storage plug-in jar package url address
     */
    private static URL[] PLUGIN_JAR_URLS;

    static {
        /*
         * Try to solve the classloader dead lock. See https://github.com/apache/skywalking/pull/2016
         */
        registerAsParallelCapable();
        LOGGER = LogManager.getLogger(AgentClassLoader.class);
        initializePlugins();
    }

    /**
     * Initialize plugin url array
     */
    private static void initializePlugins() {
        File agentDictionary;
        try {
            agentDictionary = AgentPackagePath.getPath();
        } catch (Exception e) {
            throw new RuntimeException("Can't find the root path");
        }
        List<File> classpath = new LinkedList<>();
        Config.Plugin.MOUNT.forEach(mountFolder -> classpath.add(new File(agentDictionary, mountFolder)));
        LinkedList<URL> jarFiles = doGetJars(classpath);
        PLUGIN_JAR_URLS = jarFiles.toArray(new URL[jarFiles.size()]);
    }

    /**
     * User class loader mapped to Skywalking plugin class loader, fixing osgi with a separate class loader for each bundle package
     */
    private static final Map<ClassLoader, AgentClassLoader> CLASS_LOADER_MAP = new HashMap<>();

    /**
     * Get class loader
     * @param classLoader User class loader
     * @return User class loader adds skywalking plugin
     */
    public static AgentClassLoader getClassLoader(ClassLoader classLoader) {
        return CLASS_LOADER_MAP.computeIfAbsent(classLoader, k -> new AgentClassLoader(PLUGIN_JAR_URLS, k));
    }

    /**
     * The default class loader for the agent.
     */
    private static AgentClassLoader DEFAULT_LOADER;

    public static AgentClassLoader getDefault() {
        return DEFAULT_LOADER;
    }

    /**
     * Init the default class loader.
     *
     * @throws AgentPackageNotFoundException if agent package is not found.
     */
    public static void initDefaultLoader() {
        if (DEFAULT_LOADER == null) {
            synchronized (AgentClassLoader.class) {
                if (DEFAULT_LOADER == null) {
                    DEFAULT_LOADER = new AgentClassLoader(PLUGIN_JAR_URLS, PluginBootstrap.class.getClassLoader());
                }
            }
        }
    }

    public AgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> loadedClass = super.findClass(name);
        if (Objects.isNull(loadedClass)) {
            throw new ClassNotFoundException("Can't find " + name);
        }
        return processLoadedClass(loadedClass);
    }

    private Class<?> processLoadedClass(Class<?> loadedClass) {
        final PluginConfig pluginConfig = loadedClass.getAnnotation(PluginConfig.class);
        if (pluginConfig != null) {
            // Set up the plugin config when loaded by class loader at the first time.
            // Agent class loader just loaded limited classes in the plugin jar(s), so the cost of this
            // isAssignableFrom would be also very limited.
            SnifferConfigInitializer.initializeConfig(pluginConfig.root());
        }

        return loadedClass;
    }

    private static LinkedList<URL> doGetJars(List<File> classpath) {
        LinkedList<URL> jars = new LinkedList<>();
        for (File path : classpath) {
            if (path.exists() && path.isDirectory()) {
                String[] jarFileNames = path.list((dir, name) -> name.endsWith(".jar"));
                for (String fileName : jarFileNames) {
                    try {
                        File file = new File(path, fileName);
                        jars.add(file.toURI().toURL());
                        LOGGER.info("{} loaded.", file.toString());
                    } catch (IOException e) {
                        LOGGER.error(e, "{} jar file can't be resolved", fileName);
                    }
                }
            }
        }
        return jars;
    }
}
