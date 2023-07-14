/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package test.apache.skywalking.apm.testcase.sc.gateway.projectB.controller;

import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.Random;

@RestController
public class TestController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    static String randomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int strLen = str.length();
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(strLen);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    @RequestMapping("/provider/b/testcase")
    public String testcase() {

        String d1 = randomString(10);
        String d2 = randomString(13);

        RequestEntity<Void> requestEntity = RequestEntity.get(URI.create("http://localhost:8080/provider/b/context"))
                .header("x-custom-data1", d1)
                .header("x-custom-data2", d2)
                .build();

        logger.info("requestEntity: {}", requestEntity);

        ResponseEntity<String> response = new RestTemplate().exchange(requestEntity, String.class);

        logger.info("response: {}", response);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("response status code is not 2xx");
        }

        String body = response.getBody();
        if (StringUtils.isEmpty(body)) {
            throw new IllegalStateException("response body is empty");
        }

        if (!checkResponseBody(d1, d2, body)) {
            throw new IllegalStateException("response body is not expected");
        }

        return "1";
    }

    private boolean checkResponseBody(String d1, String d2, String body) {
        String[] lines = body.split("\n");
        return lines.length == 2 && (lines[0].equals(d1) && lines[1].equals(d1 + ";" + d2));
    }

    @RequestMapping("/provider/b/context")
    public String context(@RequestHeader Map<String, String> headers) {

        String traceIdInHeader = headers.get("x-trace-id");

        logger.info("traceId:{} vs {}", TraceContext.traceId(), traceIdInHeader);

        if (!TraceContext.traceId().equals(traceIdInHeader)) {
            throw new IllegalStateException("response header x-trace-id is not expected");
        }

        if (StringUtils.isEmpty(headers.get("x-segment-id"))) {
            throw new IllegalStateException("response header x-segment-id is empty");
        }

        if (StringUtils.isEmpty(headers.get("x-span-id"))) {
            throw new IllegalStateException("response header x-span-id is empty");
        }

        return TraceContext.getCorrelation("custom-data1").orElse("") + "\n" +
        TraceContext.getCorrelation("custom-data2").orElse("");
    }

    @RequestMapping("/provider/b/healthCheck")
    public String healthCheck() {
        return "Success";
    }
}
