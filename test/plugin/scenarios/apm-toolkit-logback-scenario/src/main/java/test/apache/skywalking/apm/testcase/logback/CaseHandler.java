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

package test.apache.skywalking.apm.testcase.logback;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.skywalking.apm.toolkit.trace.CarrierItemRef;
import org.apache.skywalking.apm.toolkit.trace.ContextCarrierRef;
import org.apache.skywalking.apm.toolkit.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CaseHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseHandler.class);

    /**
     * The trace continues a fixed upstream context, so the trace ID printed in the logs is known in the expected data.
     */
    private static final String SW8 = String.join(
        "-", "1", encode("logback-scenario-trace-id"), encode("upstream-segment-id"), "1",
        encode("upstream-service"), encode("upstream-instance"), encode("/upstream"), encode("upstream:8080")
    );

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log();
        respond(exchange);
    }

    private void log() {
        ContextCarrierRef carrier = new ContextCarrierRef();
        CarrierItemRef item = carrier.items();
        while (item.hasNext()) {
            item = item.next();
            if ("sw8".equals(item.getHeadKey())) {
                item.setHeadValue(SW8);
            }
        }
        Tracer.createEntrySpan("logback-case", carrier);
        // A key of the application's own, which the SkyWalking MDC converter leaves to logback.
        MDC.put("user", "skywalking");
        try {
            LOGGER.info("logback-scenario");
        } finally {
            MDC.remove("user");
            Tracer.stopSpan();
        }
    }

    static void respond(HttpExchange exchange) throws IOException {
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        byte[] body = "Success".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
