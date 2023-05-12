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

package test.apache.skywalking.apm.testcase.grizzly;

import java.io.IOException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import test.apache.skywalking.apm.testcase.grizzly.controller.CaseHandler;
import test.apache.skywalking.apm.testcase.grizzly.controller.HealCheckHandler;
import test.apache.skywalking.apm.testcase.grizzly.controller.ReceiveContextHandler;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        HttpServer server = new HttpServer();
        NetworkListener networkListener = new NetworkListener("grizzly-scenariotest",
                "0.0.0.0", 18181);
        server.addListener(networkListener);
        server.getServerConfiguration().addHttpHandler(new CaseHandler(),
                "/grizzly-2.3.x-4.x-scenario/case/grizzly-2.3.x-4.x-scenario");
        server.getServerConfiguration()
                .addHttpHandler(new HealCheckHandler(),
                        "/grizzly-2.3.x-4.x-scenario/case/healthCheck");
        server.getServerConfiguration().addHttpHandler(new ReceiveContextHandler(),
                "/grizzly-2.3.x-4.x-scenario/case/receive-context");
        server.start();
        Thread.sleep(10000);
        System.in.read();
    }
}
