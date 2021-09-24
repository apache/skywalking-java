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

package org.apache.skywalking.testcase.httpclient5;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
@RequestMapping("/httpclient-5.x/case")
public class FrontController {

    private static final Logger LOGGER = LogManager.getLogger(FrontController.class);

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private CloseableHttpAsyncClient httpAsyncClient;

    @GetMapping("/healthcheck")
    public String healthcheck() {
        return "Success";
    }

    @GetMapping("/get")
    public String get() throws Exception {
        final HttpGet httpget = new HttpGet("http://127.0.0.1:8080/httpclient-5.x/case/asyncGet");

        LOGGER.info("Executing request " + httpget.getMethod() + " " + httpget.getUri());
        try (final CloseableHttpResponse response = httpClient.execute(httpget)) {
            LOGGER.info(response.getCode() + " -> " + response.getReasonPhrase());
            final HttpEntity entity = response.getEntity();
            try {
                return entity != null ? EntityUtils.toString(entity) : "";
            } catch (final ParseException ex) {
                throw new ClientProtocolException(ex);
            }
        }
    }

    @GetMapping("/asyncGet")
    public String asyncGet() throws Exception {
        SimpleHttpRequest request = SimpleHttpRequests.get("http://127.0.0.1:8080/httpclient-5.x/back");
        LOGGER.info("Executing async request " + request.getMethod() + " " + request.getUri());

        Future<SimpleHttpResponse> future = httpAsyncClient.execute(request, null);
        SimpleHttpResponse response = future.get();
        LOGGER.info(response.getCode() + " -> " + response.getReasonPhrase());
        return response.getBodyText();
    }
}
