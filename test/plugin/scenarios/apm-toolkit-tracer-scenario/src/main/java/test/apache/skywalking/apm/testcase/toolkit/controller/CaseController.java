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

package test.apache.skywalking.apm.testcase.toolkit.controller;

import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/case")
@Log4j2
public class CaseController {

    private static final String SUCCESS = "Success";

    @Autowired
    private TestService testService;

    @RequestMapping("/apm-toolkit-tracer-scenario")
    @ResponseBody
    public String testcase() {
        testService.startService("trace-scenario-entry");
        testService.doSomething("trace-scenario-local");

        try {
            testService.callNewThread("call-new-thread");
        } catch (Exception e) {
            // ignore
        }

        try {
            testService.asyncPrepareAndFinish();
        } catch (Exception e) {

        }

        Map<String, String> map = testService.endServiceWithCarrier("trace-scenario-exit", "127.0.0.1:5555");

        try {
            doPost("http://localhost:8080/apm-toolkit-tracer-scenario/case/startNewProcess", map);
        } catch (IOException e) {
            // ignore
        }
        return SUCCESS;
    }

    @PostMapping("/startNewProcess")
    @ResponseBody
    public String startNewProcess(@RequestParam Map<String, String> params) {
        testService.startServiceWithCarrier("start-new-process", params);
        testService.doSomething("local-in-new-process");
        Map<String, String> map = testService.endServiceWithInject("exit-new-process", "127.0.0.1:6666");
        try {
            doPost("http://localhost:8080/apm-toolkit-tracer-scenario/case/startAnotherNewProcess", map);
        } catch (IOException e) {
            // ignore
        }
        return SUCCESS;
    }

    @PostMapping("/startAnotherNewProcess")
    @ResponseBody
    public String startAnotherNewProcess(@RequestParam Map<String, String> params) {
        testService.startServiceWithExtract("start-another-new-process", params);
        testService.doSomething("local-in-another-new-process");
        testService.endService("exit-another-new-process", "127.0.0.1:8888");
        return SUCCESS;
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        return SUCCESS;
    }

    private static void doPost(String url, Map<String, String> map) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);
            List<NameValuePair> parameters = new ArrayList<>(0);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters);
            httpPost.setEntity(formEntity);
            ResponseHandler<String> responseHandler = response -> {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            };
            httpClient.execute(httpPost, responseHandler);
        } finally {
            httpClient.close();
        }
    }
}
