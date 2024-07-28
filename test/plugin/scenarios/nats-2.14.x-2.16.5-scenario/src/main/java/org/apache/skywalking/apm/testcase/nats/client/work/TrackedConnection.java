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
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.time.Duration;

@Slf4j
public class TrackedConnection {

    public static Connection newConnection(String url) throws IOException, InterruptedException {
        return Nats.connect(createOptions(url));
    }

    public static Options createOptions(String server) {
        Options.Builder builder = new Options.Builder().
                server(server).
                connectionTimeout(Duration.ofSeconds(5)).
                pingInterval(Duration.ofSeconds(10)).
                reconnectWait(Duration.ofSeconds(1)).
                maxReconnects(-1).
                traceConnection()
                .token("abcdefgh".toCharArray());

        builder = builder.connectionListener((conn, type) -> log.info("Status change " + type));
        builder = builder.errorListener(new ErrorListener() {
            @Override
            public void exceptionOccurred(Connection conn, Exception exp) {
                log.info("ATS connection exception occurred");
                exp.printStackTrace();
            }

            @Override
            public void errorOccurred(Connection conn, String error) {
                log.info("NATS connection error occurred " + error);
            }
        });

        return builder.build();
    }
}
