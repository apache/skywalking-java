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
 */

package org.apache.skywalking.apm.testcase.activemq.artemis.service;

import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MessagingListener {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @JmsListener(destination = "QueueDemo", concurrency = "10")
    public void onMessageReceived(Message message) throws Exception {
        logger.info("received normal message: " + message);
    }

    @JmsListener(destination = "TopicDemo", concurrency = "1", containerFactory = "jmsListenerContainerTopic")
    public void onSelfMessageReceived(Message message) throws Exception {
        logger.info("received self message: " + message);
    }

    @JmsListener(destination = "QueueDemo2", concurrency = "1")
    public void onSendReceiveMessageReceived(Message message) throws Exception {
        logger.info("received send and receive message: " + message);
    }

    @JmsListener(destination = "QueueDemo3", concurrency = "1")
    public void onConvertSendMessageReceived(Message message) throws Exception {
        logger.info("received convert and send message: " + message);
    }
}
