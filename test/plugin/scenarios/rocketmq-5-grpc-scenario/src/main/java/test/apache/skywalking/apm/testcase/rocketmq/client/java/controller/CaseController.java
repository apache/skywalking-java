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

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.tools.command.MQAdminStartup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RestController
@RequestMapping("/case")
@Slf4j
public class CaseController {

    private static final String SUCCESS = "Success";

    @Value("${endpoints}")
    private String endpoints;

    @Value("${nameServer}")
    private String nameServer;

    static final String TOPIC = "TopicTest";
    static final String TAG = "TagA";
    static final String GROUP = "group1";

    Producer producer;

    PushConsumer consumer;

    @RequestMapping("/rocketmq-5-grpc-scenario")
    @ResponseBody
    public String testcase() {
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                    .setEndpoints(endpoints)
                    .enableSsl(false)
                    .build();
            // start producer
            if (producer == null) {
                producer = provider.newProducerBuilder()
                        .setClientConfiguration(clientConfiguration)
                        .build();
            }

            // send msg
            Message message = provider.newMessageBuilder()
                    // Set topic for the current message.
                    .setTopic(TOPIC)
                    // Message secondary classifier of message besides topic.
                    .setTag(TAG)
                    // Key(s) of the message, another way to mark message besides message id.
                    .setKeys("KeyA")
                    .setBody("This is a normal message for Apache RocketMQ".getBytes(StandardCharsets.UTF_8))
                    .build();
            SendReceipt sendReceipt = producer.send(message);

            // start consumer
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FilterExpression filterExpression = new FilterExpression(TAG, FilterExpressionType.TAG);
                        if (consumer == null) {
                            consumer = provider.newPushConsumerBuilder()
                                    .setClientConfiguration(clientConfiguration)
                                    .setConsumerGroup(GROUP)
                                    .setSubscriptionExpressions(Collections.singletonMap(TOPIC, filterExpression))
                                    .setMessageListener(new MyConsumer())
                                    .build();
                        }
                    } catch (Exception e) {
                        log.error("consumer start error", e);
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            log.error("testcase error", e);
        }
        return SUCCESS;
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() throws Exception {
        System.setProperty(MixAll.ROCKETMQ_HOME_ENV, this.getClass().getResource("/").getPath());
        String[] subArgs = new String[]{
                "updateTopic",
                "-n",
                nameServer,
                "-c",
                "DefaultCluster",
                "-t",
                "TopicTest"};
        MQAdminStartup.main(subArgs);

        subArgs = new String[]{
                "updateSubGroup",
                "-n",
                nameServer,
                "-c",
                "DefaultCluster",
                "-g",
                "group1"};
        MQAdminStartup.main(subArgs);

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                .setEndpoints(endpoints)
                .enableSsl(false)
                .build();
        // start producer
        Producer producer = provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .build();
        return SUCCESS;
    }

    public static class MyConsumer implements MessageListener {

        @Override
        public ConsumeResult consume(MessageView messageView) {
            log.info("Consume message successfully, messageId={},messageBody={}", messageView.getMessageId(),
                    messageView.getBody().toString());
            return ConsumeResult.SUCCESS;
        }
    }

}
