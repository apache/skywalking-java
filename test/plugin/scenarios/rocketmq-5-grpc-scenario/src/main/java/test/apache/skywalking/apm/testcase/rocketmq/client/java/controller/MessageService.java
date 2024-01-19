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

package test.apache.skywalking.apm.testcase.rocketmq.client.java.controller;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.tools.command.MQAdminStartup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageService {
    @Value("${endpoints}")
    private String endpoints;

    @Value("${nameServer}")
    private String nameServer;

    public void sendNormalMessage(String topic, String tag, String group) throws ClientException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        final Producer producer = ProducerSingleton.getInstance(endpoints, topic);
        byte[] body = "This is a normal message for Apache RocketMQ".getBytes(StandardCharsets.UTF_8);
        final Message message = provider.newMessageBuilder()
                                        .setTopic(topic)
                                        .setTag(tag)
                                        .setKeys(UUID.randomUUID().toString())
                                        .setBody(body)
                                        .build();
        try {
            final SendReceipt sendReceipt = producer.send(message);
            log.info("Send normal message successfully, messageId={}", sendReceipt.getMessageId());
        } catch (Throwable t) {
            log.error("Failed to send message", t);
        }
    }

    public void sendNormalMessageAsync(String topic, String tag, String group) throws ClientException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        final Producer producer = ProducerSingleton.getInstance(endpoints, topic);
        byte[] body = "This is a async message for Apache RocketMQ".getBytes(StandardCharsets.UTF_8);
        final Message message = provider.newMessageBuilder()
                                        .setTopic(topic)
                                        .setTag(tag)
                                        .setKeys(UUID.randomUUID().toString())
                                        .setBody(body)
                                        .build();
        try {
            producer.sendAsync(message);
            log.info("Send async message successfully");
        } catch (Throwable t) {
            log.error("Failed to send message", t);
        }
    }

    public void pushConsumes(List<String> topics, List<String> tags, String group) {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                                                                     .setEndpoints(endpoints)
                                                                     .build();
        try {
            Map<String, FilterExpression> filterExpressionMap = new HashMap<>();
            for (int i = 0; i < topics.size(); i++) {
                filterExpressionMap.put(
                    topics.get(i), new FilterExpression(tags.get(i), FilterExpressionType.TAG));
            }

            PushConsumer consumer = provider.newPushConsumerBuilder()
                                            .setClientConfiguration(clientConfiguration)
                                            .setSubscriptionExpressions(filterExpressionMap)
                                            .setConsumerGroup(group)
                                            .setMessageListener(new MyConsumer())
                                            .build();
        } catch (Exception e) {
            log.error("consumer start error", e);
        }
    }

    public void simpleConsumes(List<String> topics,
                               List<String> tags,
                               String group,
                               Integer maxMessageNum,
                               Integer duration) {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                                                                     .setEndpoints(endpoints)
                                                                     .build();

        try {
            Map<String, FilterExpression> filterExpressionMap = new HashMap<>();
            for (int i = 0; i < topics.size(); i++) {
                FilterExpression filterExpression = new FilterExpression(tags.get(i), FilterExpressionType.TAG);
                filterExpressionMap.put(topics.get(i), filterExpression);
            }

            SimpleConsumer consumer = provider.newSimpleConsumerBuilder()
                                              .setClientConfiguration(clientConfiguration)
                                              .setConsumerGroup(group)
                                              .setAwaitDuration(Duration.ofSeconds(10))
                                              .setSubscriptionExpressions(filterExpressionMap)
                                              .build();

            Duration invisibleDuration = Duration.ofSeconds(duration);
            final List<MessageView> messages = consumer.receive(maxMessageNum, invisibleDuration);
            messages.forEach(messageView -> {
                log.info("Received message: {}", messageView);
            });
            for (MessageView msg : messages) {
                final MessageId messageId = msg.getMessageId();
                try {
                    consumer.ack(msg);
                    log.info("Message is acknowledged successfully, messageId={}", messageId);
                } catch (Throwable t) {
                    log.error("Message is failed to be acknowledged, messageId={}", messageId, t);
                }
            }
        } catch (Exception e) {
            log.error("consumer start error", e);
        }
    }

    public void simpleConsumeAsync(String topic,
                                   String tag,
                                   String group,
                                   Integer maxMessageNum,
                                   Integer duration) {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                                                                     .setEndpoints(endpoints)
                                                                     .build();
        try {
            Duration awaitDuration = Duration.ofSeconds(10);
            FilterExpression filterExpression = new FilterExpression(tag, FilterExpressionType.TAG);
            SimpleConsumer consumer = provider.newSimpleConsumerBuilder()
                                              .setClientConfiguration(clientConfiguration)
                                              .setConsumerGroup(group)
                                              .setAwaitDuration(awaitDuration)
                                              .setSubscriptionExpressions(
                                                  Collections.singletonMap(topic, filterExpression))
                                              .build();
            Duration invisibleDuration = Duration.ofSeconds(duration);
            ExecutorService receiveCallbackExecutor = Executors.newCachedThreadPool();
            ExecutorService ackCallbackExecutor = Executors.newCachedThreadPool();
            final CompletableFuture<List<MessageView>> future0 = consumer.receiveAsync(
                maxMessageNum,
                invisibleDuration
            );
            future0.whenCompleteAsync((messages, throwable) -> {
                if (null != throwable) {
                    log.error("Failed to receive message from remote", throwable);
                    return;
                }
                log.info("Received {} message(s)", messages.size());
                final Map<MessageView, CompletableFuture<Void>> map =
                    messages.stream().collect(Collectors.toMap(message -> message, consumer::ackAsync));
                for (Map.Entry<MessageView, CompletableFuture<Void>> entry : map.entrySet()) {
                    final MessageId messageId = entry.getKey().getMessageId();
                    final CompletableFuture<Void> future = entry.getValue();
                    future.whenCompleteAsync((v, t) -> {
                        if (null != t) {
                            log.error("Message is failed to be acknowledged, messageId={}", messageId, t);
                            return;
                        }
                        log.info("Message is acknowledged successfully, messageId={}", messageId);
                    }, ackCallbackExecutor);
                }
            }, receiveCallbackExecutor);
        } catch (Exception e) {
            log.error("consumer start error", e);
        }
    }

    public void updateNormalTopic(String topic) {
        String[] subArgs = new String[] {
            "updateTopic",
            "-n",
            nameServer,
            "-c",
            "DefaultCluster",
            "-t",
            topic,
            "-a",
            "+message.type=NORMAL"
        };
        MQAdminStartup.main(subArgs);
    }

    public static class MyConsumer implements MessageListener {

        @Override
        public ConsumeResult consume(MessageView messageView) {
            log.info("Consume message successfully, messageId={},messageBody={}", messageView.getMessageId(),
                     messageView.getBody().toString()
            );
            return ConsumeResult.SUCCESS;
        }
    }

}
