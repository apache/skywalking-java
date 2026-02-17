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

package test.apache.skywalking.apm.testcase.rocketmq.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/case")
@Slf4j
public class CaseController {

    private static final String SUCCESS = "Success";

    @Value("${name.server}")
    private String namerServer;

    private volatile boolean consumerStarted = false;
    private volatile boolean consumerReady = false;
    private DefaultMQProducer probeProducer;

    @RequestMapping("/rocketmq-scenario")
    @ResponseBody
    public String testcase() {
        try {
            DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
            producer.setNamesrvAddr(namerServer);
            producer.start();
            System.out.printf("Provider Started.%n");

            Message msg = new Message("TopicTest",
                    ("Hello RocketMQ sendMsg " + new Date()).getBytes(RemotingHelper.DEFAULT_CHARSET)
            );
            msg.setTags("TagA");
            msg.setKeys("KeyA");
            SendResult sendResult = producer.send(msg);
            System.out.printf("%s send msg: %s%n", new Date(), sendResult);
        } catch (Exception e) {
            log.error("testcase error", e);
        }
        return SUCCESS;
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() throws Exception {
        if (!consumerStarted) {
            // Set flag early to prevent re-entry from concurrent healthCheck
            // requests (each curl has a 3s timeout, and initialization may
            // take longer than that).
            consumerStarted = true;
            try {
                // Speed up client-side rebalance from default 20s to 2s so the
                // consumer discovers topic queues faster after startup.
                System.setProperty("rocketmq.client.rebalance.waitInterval", "2000");

                // Start a producer that stays alive to send probe messages on each retry.
                probeProducer = new DefaultMQProducer("healthCheck_please_rename_unique_group_name");
                probeProducer.setNamesrvAddr(namerServer);
                probeProducer.start();
                Message probeMsg = new Message("TopicTest", "probe".getBytes(StandardCharsets.UTF_8));
                probeProducer.send(probeMsg);
                System.out.printf("HealthCheck: Topic created via probe message.%n");

                // Start consumer after topic exists so rebalance finds queues immediately.
                DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name");
                consumer.setNamesrvAddr(namerServer);
                consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                consumer.subscribe("TopicTest", "*");
                consumer.registerMessageListener(new MessageListenerConcurrently() {
                    @Override
                    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                        System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), new String(msgs.get(0).getBody(), StandardCharsets.UTF_8));
                        consumerReady = true;
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }
                });
                consumer.start();
                System.out.printf("Consumer Started.%n");
            } catch (Exception e) {
                consumerStarted = false;
                throw e;
            }
        }

        // Wait until the consumer has actually received a probe message,
        // confirming rebalance is complete and it can consume from the topic.
        // Send a fresh probe on every retry so the consumer picks it up once
        // rebalance finishes (messages sent before rebalance may never arrive).
        if (!consumerReady) {
            try {
                Message probeMsg = new Message("TopicTest", "probe".getBytes(StandardCharsets.UTF_8));
                probeProducer.send(probeMsg);
                System.out.printf("HealthCheck: sent probe message (consumer not ready yet).%n");
            } catch (Exception e) {
                System.out.printf("HealthCheck: failed to send probe: %s%n", e.getMessage());
            }
            throw new RuntimeException("Consumer has not received probe message yet");
        }

        return SUCCESS;
    }

}
