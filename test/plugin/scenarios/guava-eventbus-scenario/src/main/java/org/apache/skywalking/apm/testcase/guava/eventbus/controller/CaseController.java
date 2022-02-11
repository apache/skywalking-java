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

package org.apache.skywalking.apm.testcase.guava.eventbus.controller;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import java.util.concurrent.Executors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.skywalking.apm.testcase.guava.eventbus.service.SubscriberService;
import org.apache.skywalking.apm.testcase.guava.eventbus.service.TestEvent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/case")
public class CaseController {

    public static final String SUCCESS = "Success";
    private final EventBus eventBus = new EventBus();
    private final AsyncEventBus asyncEventBus = new AsyncEventBus(Executors.newFixedThreadPool(5));
    @Resource
    private SubscriberService subscriberService;

    @RequestMapping("/guava-scenario")
    @ResponseBody
    public void testcase(HttpServletRequest request) throws Exception {
        eventBus.register(subscriberService);
        asyncEventBus.register(subscriberService);
        final TestEvent testEvent = new TestEvent();
        testEvent.setContent("test");
        eventBus.post(testEvent);
        testEvent.setAsyncContext(request.startAsync());
        asyncEventBus.post(testEvent);
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() throws Exception {
        return SUCCESS;
    }

}
