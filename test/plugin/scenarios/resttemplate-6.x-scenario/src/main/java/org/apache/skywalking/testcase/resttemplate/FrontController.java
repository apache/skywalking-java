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

package org.apache.skywalking.testcase.resttemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/resttemplate/case")
public class FrontController {

    @Value("${server.port:8080}")
    int port;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping(value = "/healthcheck")
    public String healthcheck() {
        return "Success";
    }

    @GetMapping("/resttemplate")
    public String front() {
        syncRequest("http://localhost:" + port + "/resttemplate-6.x-scenario/resttemplate/syncback");
        return "Success";
    }

    private String syncRequest(String url) {
        restTemplate.getForObject(url, String.class);

        return "Success";
    }
}
