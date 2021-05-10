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

package test.apache.skywalking.apm.testcase.pulsar.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/case")
@PropertySource("classpath:application.properties")
public class CaseController {

    private static final Logger LOGGER = LogManager.getLogger(CaseController.class);

    private static final String PULSAR_DOMAIN = "pulsar://";

    @Value("${service.url:127.0.0.1:6650}")
    private String serviceUrl;

    @RequestMapping("/pulsar-case")
    @ResponseBody
    public String pulsarCase() {

        String topicOnePartition = "test";
        String topicMultiPartition = "testMultiPartition";

        try {
            doSendAndReceiveMessage(topicOnePartition);

            createTopic(topicMultiPartition, 2);
            doSendAndReceiveMessage(topicMultiPartition);
        } catch (IOException e) {
            LOGGER.error("test error", e);
        }

        return "Success";
    }

    private void doSendAndReceiveMessage(String topic) throws PulsarClientException {
        CountDownLatch latch = new CountDownLatch(2);

        PulsarClient pulsarClient = PulsarClient.builder().serviceUrl(PULSAR_DOMAIN + serviceUrl).build();

        Producer<byte[]> producer = pulsarClient.newProducer().topic(topic).create();

        Consumer<byte[]> consumer = pulsarClient.newConsumer().topic(topic).subscriptionName("test").subscribe();

        final Consumer<byte[]> consumerWithListener = pulsarClient.newConsumer().topic(topic)
                .subscriptionName("testWithListener")
                .messageListener((c, msg) -> {
                    try {
                        if (msg != null) {
                            String propertiesFormat = "key = %s, value = %s";
                            StringBuilder builder = new StringBuilder();
                            msg.getProperties()
                                    .forEach((k, v) -> builder.append(String.format(propertiesFormat, k, v))
                                            .append(", "));
                            LOGGER.info(
                                    "Received message with messageId = {}, key = {}, value = {}, properties = {}",
                                    msg.getMessageId(), msg
                                            .getKey(), new String(msg.getValue()), builder.toString());

                        }
                        c.acknowledge(msg);
                    } catch (Exception e) {
                        LOGGER.error("Receive message error", e);
                    } finally {
                        latch.countDown();
                    }
                }).subscribe();

        producer.newMessage().key("testKey").value(Integer.toString(1).getBytes()).property("TEST", "TEST").send();

        Thread t = new Thread(() -> {
            try {
                Message<byte[]> msg = consumer.receive(3, TimeUnit.SECONDS);
                if (msg != null) {
                    String propertiesFormat = "key = %s, value = %s";
                    StringBuilder builder = new StringBuilder();
                    msg.getProperties()
                            .forEach((k, v) -> builder.append(String.format(propertiesFormat, k, v)).append(", "));
                    LOGGER.info("Received message with messageId = {}, key = {}, value = {}, properties = {}",
                            msg.getMessageId(), msg
                                    .getKey(), new String(msg.getValue()), builder.toString());

                }
                consumer.acknowledge(msg);
            } catch (PulsarClientException e) {
                LOGGER.error("Receive message error", e);
            } finally {
                latch.countDown();
            }
        });

        t.start();

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Can get message from consumer", e);
            t.interrupt();
        }

        producer.close();
        consumer.close();
        consumerWithListener.close();
    }

    private void createTopic(String topic, int numOfPartitions) throws IOException {
        final URL url = new URL("http://pulsar-standalone:8080/admin/v2/persistent/public/default/" + topic + "/partitions");
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);

        final OutputStream outputStream = connection.getOutputStream();
        outputStream.write(String.valueOf(numOfPartitions).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();

        LOGGER.info("Create topic result:{}", connection.getResponseCode());
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() throws InterruptedException {
        try (PulsarClient pulsarClient = PulsarClient.builder()
                .serviceUrl(PULSAR_DOMAIN + serviceUrl)
                .build(); Producer<byte[]> producer = pulsarClient.newProducer()
                .topic("healthCheck")
                .create()) {
            if (producer.isConnected()) {
                return "Success";
            } else {
                throw new RuntimeException("Health check error, the reason is test producer is disconnected!");
            }
        } catch (PulsarClientException e) {
            if (e instanceof PulsarClientException.BrokerMetadataException) {
                // Broker is not ready, retry here
                Thread.sleep(1000);
                return healthCheck();
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
