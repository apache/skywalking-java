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

package test.apache.skywalking.apm.testcase.shenyu.entry.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * http entry service.
 */
@RestController
@RequestMapping("/entry")
public class HttpEntryController {

    private static final String GATEWAY_SERVICE_URL = "http://localhost:9196/grpc/echo";

    private final RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/check")
    public String healthCheck() {
        return "ok";
    }

    @GetMapping(value = "/rpc-service", produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<String> httpService() throws Exception {
        TimeUnit.MILLISECONDS.sleep(100);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "hello world");
        Map<String, Object> body = new HashMap<>();
        body.put("data", Collections.singletonList(data));

        ResponseEntity<String> entity = restTemplate.postForEntity(GATEWAY_SERVICE_URL, body, String.class);

        if (entity.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok(Optional.ofNullable(entity.getBody()).orElse(""));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("failed");
    }

}
