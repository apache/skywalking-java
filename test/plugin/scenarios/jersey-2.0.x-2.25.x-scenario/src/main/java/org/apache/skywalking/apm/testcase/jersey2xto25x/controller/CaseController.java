/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.testcase.jersey2xto25x.controller;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("case")
public class CaseController {

    @Path("jersey-2.0.x-2.25.x-scenario")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        // use okhttp request
        Request request = new Request.Builder().url(
                        "http://127.0.0.1:18080/jersey-2.0.x-2.25.x-scenario/case/receiveContext")
                .build();
        try {
            new OkHttpClient().newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "Got it!";
    }

    @Path("healthCheck")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String helloWorld() {
        return "Hello";
    }

    @Path("receiveContext")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "test";
    }

}

