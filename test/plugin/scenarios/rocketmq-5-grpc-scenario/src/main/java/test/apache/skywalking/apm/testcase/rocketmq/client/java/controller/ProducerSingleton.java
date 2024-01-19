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

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.ProducerBuilder;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;

public class ProducerSingleton {
    private static volatile Producer PRODUCER;

    private ProducerSingleton() {
    }

    private static Producer buildProducer(TransactionChecker checker,
                                          String endpoints,
                                          String... topics) throws ClientException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                                                                     .setEndpoints(endpoints)
                                                                     .enableSsl(false)
                                                                     .build();
        final ProducerBuilder builder = provider.newProducerBuilder()
                                                .setClientConfiguration(clientConfiguration)
                                                .setTopics(topics);
        if (checker != null) {
            builder.setTransactionChecker(checker);
        }
        return builder.build();
    }

    public static Producer getInstance(String endpoints, String... topics) throws ClientException {
        if (null == PRODUCER) {
            synchronized (ProducerSingleton.class) {
                if (null == PRODUCER) {
                    PRODUCER = buildProducer(null, endpoints, topics);
                }
            }
        }
        return PRODUCER;
    }
}
