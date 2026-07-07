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

package org.apache.skywalking.apm.testcase.jetty12xserver;

import org.apache.skywalking.apm.testcase.jetty12xserver.handler.CaseHandler;
import org.eclipse.jetty.server.Server;

/**
 * Embedded Jetty 12 server. Jetty 12 handling routes every request through
 * {@code org.eclipse.jetty.server.Server#handle(Request, Response, Callback)} — the method the
 * jetty-server-12.x plugin enhances — before delegating to the top-level {@link CaseHandler}.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        Server server = new Server(18080);
        server.setHandler(new CaseHandler());
        server.start();
        server.join();
    }
}
