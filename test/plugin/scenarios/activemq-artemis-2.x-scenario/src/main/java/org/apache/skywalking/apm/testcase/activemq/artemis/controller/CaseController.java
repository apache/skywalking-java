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
 */

package org.apache.skywalking.apm.testcase.activemq.artemis.controller;

import org.apache.skywalking.apm.testcase.activemq.artemis.service.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/testcase")
public class CaseController {
    private static final String SUCCESS = "Success";

    @Autowired
    private MessagingService messagingService;

    @GetMapping("/activemq-artemis-scenario")
    public String demo() throws Exception {
        messagingService.sendMessage("hello world");
        messagingService.sendTopicMessage("hello world");
        messagingService.sendAndReceiveMessage("hello world");
        messagingService.convertAndSendMessage("hello world");
        return SUCCESS;
    }

    @GetMapping("/healthCheck")
    public String healthCheck() {
        return SUCCESS;
    }
}
