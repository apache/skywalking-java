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

package test.apache.skywalking.apm.testcase.grizzly.workthreadpool.controller;

import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

public class CaseHandler extends HttpHandler {

    @Override
    public void service(Request request, Response response) throws Exception {

        com.squareup.okhttp.Request okhttpRequest = new com.squareup.okhttp.Request.Builder().url(
                        "http://127.0.0.1:18181/grizzly-2.3.x-4.x-workthreadpool-scenario/case/receive-context")
                .build();
        try {
            new OkHttpClient().newCall(okhttpRequest).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String hello = "hello";
        response.setContentType("text/plain");
        response.setContentLength(hello.length());
        response.getWriter().write(hello);
    }
}
