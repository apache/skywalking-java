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

package org.apache.skywalking.oap.server.configuration.zookeeper.it;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.skywalking.apm.util.PropertyPlaceholderHelper;
import org.apache.skywalking.oap.server.library.module.ApplicationConfiguration;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.library.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ITZookeeperConfigurationTest {
    private final Yaml yaml = new Yaml();

    private MockZookeeperConfigurationProvider provider;

    @Before
    public void setUp() throws Exception {
        final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        loadConfig(applicationConfiguration);

        final ModuleManager moduleManager = new ModuleManager();
        moduleManager.init(applicationConfiguration);

        provider = (MockZookeeperConfigurationProvider) moduleManager.find(MockZookeeperConfigurationModule.NAME)
                                                                     .provider();

        assertNotNull(provider);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test(timeout = 20000)
    public void shouldReadUpdated() throws Exception {
        String nameSpace = "/default";
        String key = "test-module.default.testKey";
        assertNull(provider.watcher.value());

        String zkAddress = System.getProperty("zk.address");
        log.info("zkAddress: " + zkAddress);

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(zkAddress, retryPolicy);
        client.start();
        log.info("per path: " + nameSpace + "/" + key);

        assertTrue(client.create().creatingParentsIfNeeded().forPath(nameSpace + "/" + key, "500".getBytes()) != null);

        log.info("data: " + new String(client.getData().forPath(nameSpace + "/" + key)));

        for (String v = provider.watcher.value(); v == null; v = provider.watcher.value()) {
        }

        assertTrue(client.delete().forPath(nameSpace + "/" + key) == null);

        for (String v = provider.watcher.value(); v != null; v = provider.watcher.value()) {
        }

        assertNull(provider.watcher.value());
    }

    @Test(timeout = 20000)
    public void shouldReadUpdated4GroupConfig() throws Exception {
        String nameSpace = "/default";
        String key = "test-module.default.testKeyGroup";
        assertEquals("{}", provider.groupWatcher.groupItems().toString());

        String zkAddress = System.getProperty("zk.address");
        log.info("zkAddress: " + zkAddress);

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(zkAddress, retryPolicy);
        client.start();
        log.info("per path: " + nameSpace + "/" + key);

        assertTrue(client.create().creatingParentsIfNeeded().forPath(nameSpace + "/" + key + "/item1", "100".getBytes()) != null);
        assertTrue(client.create().creatingParentsIfNeeded().forPath(nameSpace + "/" + key + "/item2", "200".getBytes()) != null);

        log.info("data: " + new String(client.getData().forPath(nameSpace + "/" + key + "/item1")));
        log.info("data: " + new String(client.getData().forPath(nameSpace + "/" + key + "/item2")));

        for (String v = provider.groupWatcher.groupItems().get("item1"); v == null; v = provider.groupWatcher.groupItems().get("item1")) {
        }
        for (String v = provider.groupWatcher.groupItems().get("item2"); v == null; v = provider.groupWatcher.groupItems().get("item2")) {
        }

        assertTrue(client.delete().forPath(nameSpace + "/" + key + "/item1") == null);
        assertTrue(client.delete().forPath(nameSpace + "/" + key + "/item2") == null);

        for (String v = provider.groupWatcher.groupItems().get("item1"); v != null; v = provider.groupWatcher.groupItems().get("item1")) {
        }
        for (String v = provider.groupWatcher.groupItems().get("item2"); v != null; v = provider.groupWatcher.groupItems().get("item2")) {
        }

        assertNull(provider.groupWatcher.groupItems().get("item1"));
        assertNull(provider.groupWatcher.groupItems().get("item2"));
    }

    @SuppressWarnings("unchecked")
    private void loadConfig(ApplicationConfiguration configuration) throws FileNotFoundException {
        Reader applicationReader = ResourceUtils.read("application.yml");
        Map<String, Map<String, Map<String, ?>>> moduleConfig = yaml.loadAs(applicationReader, Map.class);
        if (CollectionUtils.isNotEmpty(moduleConfig)) {
            moduleConfig.forEach((moduleName, providerConfig) -> {
                if (providerConfig.size() > 0) {
                    ApplicationConfiguration.ModuleConfiguration moduleConfiguration = configuration.addModule(moduleName);
                    providerConfig.forEach((name, propertiesConfig) -> {
                        Properties properties = new Properties();
                        if (propertiesConfig != null) {
                            propertiesConfig.forEach((key, value) -> {
                                properties.put(key, value);
                                final Object replaceValue = yaml.load(PropertyPlaceholderHelper.INSTANCE.replacePlaceholders(value + "", properties));
                                if (replaceValue != null) {
                                    properties.replace(key, replaceValue);
                                }
                            });
                        }
                        moduleConfiguration.addProviderConfiguration(name, properties);
                    });
                }
             });
        }
    }
}
