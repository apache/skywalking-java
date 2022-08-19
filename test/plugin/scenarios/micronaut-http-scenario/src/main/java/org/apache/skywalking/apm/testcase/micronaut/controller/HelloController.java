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

package org.apache.skywalking.apm.testcase.micronaut.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;

@Controller("/micronaut")
public class HelloController {
    @Inject
    @Client("http://localhost:8081")
    private HttpClient client;

    @Get(value = "healthCheck")
    public String checkHealth() {
        return "checked";
    }

    @Get(produces = MediaType.TEXT_PLAIN, value = "start")
    public String start() throws InterruptedException {
        try {
            client.toBlocking().retrieve(HttpRequest.GET("/micronaut/success?a=1&b=2"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            client.toBlocking().retrieve(HttpRequest.GET("/micronaut/404"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            client.toBlocking().retrieve(HttpRequest.GET("/micronaut/error"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "end";
    }

    @Get(produces = MediaType.TEXT_PLAIN, value = "success")
    public String success() {
        return "success";
    }

    @Get(produces = MediaType.TEXT_PLAIN, value = "error")
    public String error() {
        throw new NullPointerException();
    }
}
