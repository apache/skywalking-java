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

    @RequestMapping("/rocketmq-scenario")
    @ResponseBody
    public String testcase() {
        try {
            // start producer
            DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
            producer.setNamesrvAddr(namerServer);
            producer.start();
            System.out.printf("Provider Started.%n");

            // send msg
            Message msg = new Message("TopicTest",
                    ("Hello RocketMQ sendMsg " + new Date()).getBytes(RemotingHelper.DEFAULT_CHARSET)
            );
            msg.setTags("TagA");
            msg.setKeys("KeyA");
            SendResult sendResult = producer.send(msg);
            System.out.printf("%s send msg: %s%n", new Date(), sendResult);
            
            // start consumer
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name");
                        consumer.setNamesrvAddr(namerServer);
                        consumer.subscribe("TopicTest", "*");
                        consumer.registerMessageListener(new MessageListenerConcurrently() {
                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), new String(msgs.get(0).getBody(), StandardCharsets.UTF_8));
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }
                        });
                        consumer.start();
                        System.out.printf("Consumer Started.%n");
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
        // start producer
        DefaultMQProducer producer = new DefaultMQProducer("healthCheck_please_rename_unique_group_name");
        producer.setNamesrvAddr(namerServer);
        producer.start();
        System.out.printf("HealthCheck Provider Started.%n");
        return SUCCESS;
    }

}
