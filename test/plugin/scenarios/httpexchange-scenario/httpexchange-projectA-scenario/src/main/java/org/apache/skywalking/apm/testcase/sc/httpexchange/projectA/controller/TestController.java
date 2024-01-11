/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.testcase.sc.httpexchange.projectA.controller;

import java.io.IOException;
import org.apache.skywalking.apm.testcase.sc.httpexchange.projectA.api.TestcaseClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private TestcaseClient testcaseClient;

    @RequestMapping("/testcase")
    public String testcase() {
        testcaseClient.success();
//        try {
//            testcaseClient.error();
//        } catch (Exception e) {
//            //mock up error request
//        }
        testcaseClient.urlParams("urltest");
        testcaseClient.bodyParams("bodytest");
        return "test";
    }

    @RequestMapping("/healthCheck")
    public String healthCheck() throws IOException {
        testcaseClient.healthCheck();
        return "test";
    }

}
