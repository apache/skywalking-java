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

package org.apache.skywalking.apm.testcase.spring.rabbitmq.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/case")
public class CaseController {

    private static final Logger LOGGER = LogManager.getLogger(CaseController.class);

    private static final String QUEUE_NAME = "test";
    private static final String BATCH_QUEUE_NAME = "test-batch";

    private static final String MESSAGE = "rabbitmq-testcase";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RequestMapping("/rabbitmq")
    @ResponseBody
    public String send() {
        LOGGER.info("Message being published -------------->" + MESSAGE);
        rabbitTemplate.convertAndSend(QUEUE_NAME, MESSAGE);
        LOGGER.info("Message has been published-------------->" + MESSAGE);

        // Also send batch messages to test batch consumption
        LOGGER.info("Sending batch messages --------------");
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Message message = MessageBuilder.withBody(("batch-message-" + i).getBytes()).build();
            messages.add(message);
        }
        for (Message message : messages) {
            rabbitTemplate.send(BATCH_QUEUE_NAME, message);
        }
        LOGGER.info("Batch messages have been published--------------");

        return "Success";
    }

    @RabbitListener(queues = QUEUE_NAME)
    public void consumer(String message) {
        LOGGER.info("Message Consumer received-------------->" + message);
    }

    @RabbitListener(queues = BATCH_QUEUE_NAME, containerFactory = "batchRabbitListenerContainerFactory")
    public void batchConsumer(List<String> messages) {
        LOGGER.info("Batch Consumer received " + messages.size() + " messages: " + messages);
    }

    @RequestMapping("/healthcheck")
    public String healthCheck() {
        return "Success";
    }
}
