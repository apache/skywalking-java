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

package org.apache.skywalking.apm.agent.core.kafka;

import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class KafkaProducerManagerTest {
    @Test
    public void testAddListener() throws Exception {
        KafkaProducerManager kafkaProducerManager = new KafkaProducerManager();
        AtomicInteger counter = new AtomicInteger();
        int times = 100;
        for (int i = 0; i < times; i++) {
            kafkaProducerManager.addListener(new MockListener(counter));
        }
        Method notifyListeners = kafkaProducerManager
            .getClass()
            .getDeclaredMethod("notifyListeners", KafkaConnectionStatus.class);
        notifyListeners.setAccessible(true);
        notifyListeners.invoke(kafkaProducerManager, KafkaConnectionStatus.CONNECTED);

        assertEquals(counter.get(), times);
    }

    @Test
    public void testFormatTopicNameThenRegister() {
        KafkaProducerManager kafkaProducerManager = new KafkaProducerManager();
        KafkaReporterPluginConfig.Plugin.Kafka.NAMESPACE = "product";
        String value = kafkaProducerManager.formatTopicNameThenRegister(KafkaReporterPluginConfig.Plugin.Kafka.TOPIC_METRICS);
        String expectValue = KafkaReporterPluginConfig.Plugin.Kafka.NAMESPACE + "-" + KafkaReporterPluginConfig.Plugin.Kafka.TOPIC_METRICS;
        assertEquals(value, expectValue);

        KafkaReporterPluginConfig.Plugin.Kafka.NAMESPACE = "";
        value = kafkaProducerManager.formatTopicNameThenRegister(KafkaReporterPluginConfig.Plugin.Kafka.TOPIC_METRICS);
        assertEquals(KafkaReporterPluginConfig.Plugin.Kafka.TOPIC_METRICS, value);
    }

    @Test
    public void testDecrypt() throws Exception {
        KafkaReporterPluginConfig.Plugin.Kafka.DECRYPT_CLASS = "org.apache.skywalking.apm.agent.core.kafka.KafkaProducerManagerTest$DecryptTool";
        KafkaReporterPluginConfig.Plugin.Kafka.DECRYPT_METHOD = "decrypt";
        KafkaProducerManager kafkaProducerManager = new KafkaProducerManager();

        Map<String, String> config = new HashMap<>();
        String value = "test.99998888";
        config.put("test.password", Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));

        Method decryptMethod = kafkaProducerManager.getClass().getDeclaredMethod("decrypt", Map.class);
        decryptMethod.setAccessible(true);
        Map<String, String> encryptedConfig = (Map<String, String>) decryptMethod.invoke(kafkaProducerManager, config);

        assertEquals(value, encryptedConfig.get("test.password"));
    }

    static class MockListener implements KafkaConnectionStatusListener {

        private AtomicInteger counter;

        public MockListener(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void onStatusChanged(KafkaConnectionStatus status) {
            counter.incrementAndGet();
        }
    }

    static class DecryptTool {
        public Map<String, String> decrypt(Map<String, String> config) {
            if (config.containsKey("test.password")) {
                config.put("test.password", new String(Base64.getDecoder().decode(config.get("test.password")), StandardCharsets.UTF_8));
            }
            return config;
        }
    }

}
