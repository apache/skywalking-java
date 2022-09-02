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

package org.apache.skywalking.apm.testcase.nats.client.publisher;

import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class ReqReplyPublisher implements Publisher {

    @Override
    public void publish(Connection connection, String msg, String subject) {
        try {
            log.info("send message : {} to {}", msg, subject);
            connection.request(buildMsg(subject, msg), Duration.ofMinutes(5));
            log.info("receive reply message : {} from {}'reply", msg, subject);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
