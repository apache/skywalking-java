/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.testcase.nats.client.work;

import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.testcase.nats.client.publisher.Publisher;
import org.apache.skywalking.apm.testcase.nats.client.subscriber.Consumer;

@Slf4j
public class WorkBuilder {
    private final String message;
    private final String subject;
    private final String url;

    public WorkBuilder(String message, String subject, String url) {
        this.message = message;
        this.subject = subject;
        this.url = url;
    }

    public Work build(Publisher publisher, Consumer consumer) {

        return new Work() {
            @Override
            public void subscribe() {
                Connection consumerCon = WorkBuilder.this.createConnection();
                consumer.subscribe(consumerCon, WorkBuilder.this.subject);
            }

            @Override
            public void publish() {
                Connection connection = WorkBuilder.this.createConnection();
                publisher.publish(connection, message, WorkBuilder.this.subject);
            }
        };
    }

    private Connection createConnection() {
        Connection connection;
        try {
            connection = TrackedConnection.newConnection(WorkBuilder.this.url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public interface Work {
        // first subscribe
        void subscribe();

        // then publish message
        void publish();
    }

}
