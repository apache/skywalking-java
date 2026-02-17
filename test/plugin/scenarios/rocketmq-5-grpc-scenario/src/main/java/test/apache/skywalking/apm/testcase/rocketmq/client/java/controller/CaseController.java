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

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.common.MixAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/case")
@Slf4j
public class CaseController {

    private static final String SUCCESS = "Success";
    private static final String NORMAL_TOPIC = "NormalTopicTest";
    private static final String ASYNC_PRODUCER_TOPIC = "ProducerAsyncTopicTest";
    private static final String ASYNC_CONSUMER_TOPIC = "ConsumerAsyncTopicTest";
    private static final String TAG_NOMARL = "Tag:normal";
    private static final String TAG_ASYNC_PRODUCER = "Tag:async:producer";
    private static final String TAG_ASYNC_CONSUMER = "Tag:async:consumer";
    private static final String GROUP = "group1";

    @Value("${endpoints}")
    private String endpoints;

    @Autowired
    private MessageService messageService;

    private volatile boolean consumerStarted = false;

    @RequestMapping("/rocketmq-5-grpc-scenario")
    @ResponseBody
    public String testcase() {
        try {
            messageService.sendNormalMessage(NORMAL_TOPIC, TAG_NOMARL, GROUP);

            messageService.sendNormalMessageAsync(ASYNC_PRODUCER_TOPIC, TAG_ASYNC_PRODUCER, GROUP);
            messageService.sendNormalMessageAsync(ASYNC_PRODUCER_TOPIC, TAG_ASYNC_PRODUCER, GROUP);
            new Thread(() -> messageService.simpleConsumes(
                Collections.singletonList(ASYNC_PRODUCER_TOPIC),
                Collections.singletonList(TAG_ASYNC_PRODUCER), GROUP,
                10, 10
            )).start();

            messageService.sendNormalMessage(ASYNC_CONSUMER_TOPIC, TAG_ASYNC_CONSUMER, GROUP);
            messageService.sendNormalMessage(ASYNC_CONSUMER_TOPIC, TAG_ASYNC_CONSUMER, GROUP);
            new Thread(() -> messageService.simpleConsumeAsync(
                ASYNC_CONSUMER_TOPIC, TAG_ASYNC_CONSUMER, GROUP, 10, 10
            )).start();
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
                System.setProperty(MixAll.ROCKETMQ_HOME_ENV, this.getClass().getResource("/").getPath());
                messageService.updateNormalTopic(NORMAL_TOPIC);
                messageService.updateNormalTopic(ASYNC_PRODUCER_TOPIC);
                messageService.updateNormalTopic(ASYNC_CONSUMER_TOPIC);
                // Start push consumer early so it has time to receive messages
                messageService.pushConsumes(
                    Collections.singletonList(NORMAL_TOPIC),
                    Collections.singletonList(TAG_NOMARL),
                    GROUP
                );
                final Producer producer = ProducerSingleton.getInstance(endpoints, NORMAL_TOPIC);
                // Send a probe message so the consumer has something to receive
                messageService.sendNormalMessage(NORMAL_TOPIC, TAG_NOMARL, GROUP);
            } catch (Exception e) {
                consumerStarted = false;
                throw e;
            }
        }

        // Wait until the consumer has actually received a probe message,
        // confirming it can consume from the topic.
        // Send a fresh probe on every retry so the consumer picks it up once
        // rebalance finishes (messages sent before rebalance may never arrive).
        if (!MessageService.CONSUMER_READY) {
            try {
                messageService.sendNormalMessage(NORMAL_TOPIC, TAG_NOMARL, GROUP);
                System.out.printf("HealthCheck: sent probe message (consumer not ready yet).%n");
            } catch (Exception e) {
                System.out.printf("HealthCheck: failed to send probe: %s%n", e.getMessage());
            }
            throw new RuntimeException("Consumer has not received probe message yet");
        }

        return SUCCESS;
    }
}
