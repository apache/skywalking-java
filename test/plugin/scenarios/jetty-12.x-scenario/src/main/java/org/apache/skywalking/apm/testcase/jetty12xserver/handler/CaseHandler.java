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

package org.apache.skywalking.apm.testcase.jetty12xserver.handler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 * Top-level Jetty 12 core handler.
 *
 * The entry service {@code /jetty-12.x-scenario/case/jetty-12.x-scenario} makes a nested internal
 * call to {@code /jetty-12.x-scenario/case/case1} on the same server. This exercises both sides of
 * the jetty-server-12.x plugin in one trace: the entry span for the inbound request, the HttpClient
 * exit span for the outbound call (inject), and a second entry span on {@code case1} that extracts
 * the propagated context into a cross-process ref (the receiving/extract side). The health check
 * {@code /case/healthCheck} and the nested {@code /case/case1} endpoint simply return 200.
 */
public class CaseHandler extends Handler.Abstract {

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String path = request.getHttpURI().getPath();
        if (path != null && path.endsWith("/case/jetty-12.x-scenario")) {
            visit("http://localhost:18080/jetty-12.x-scenario/case/case1");
        }
        response.setStatus(200);
        response.write(true, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), callback);
        return true;
    }

    private static void visit(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url);
            ResponseHandler<String> responseHandler = response -> {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            };
            httpClient.execute(httpget, responseHandler);
        }
    }
}
