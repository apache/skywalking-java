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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

@Component
public class MessagingService {
    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendMessage(String text) throws Exception {
        jmsTemplate.send("QueueDemo", new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(text);
            }
        });
    }

    public void sendTopicMessage(String text) throws Exception {
        ActiveMQDestination destination = ActiveMQDestination.createDestination(
            "TopicDemo", ActiveMQDestination.TYPE.TOPIC);
        jmsTemplate.send(destination, new MessageCreator() {
            @Override
            public Message createMessage(final Session session) throws JMSException {
                return session.createTextMessage(text);
            }
        });
    }

    public void sendAndReceiveMessage(String text) throws Exception {
        jmsTemplate.setReceiveTimeout(500);
        jmsTemplate.sendAndReceive("QueueDemo2", new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(text);
            }
        });
    }

    public void convertAndSendMessage(String text) throws Exception {
        ActiveMQDestination destination = ActiveMQDestination.createDestination(
            "QueueDemo3", ActiveMQDestination.TYPE.QUEUE);
        jmsTemplate.convertAndSend(destination, text);
    }
}