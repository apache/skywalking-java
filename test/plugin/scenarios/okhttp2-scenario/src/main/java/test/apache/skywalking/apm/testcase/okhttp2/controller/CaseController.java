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

package test.apache.skywalking.apm.testcase.okhttp2.controller;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/case")
@Log4j2
public class CaseController {

    private static final String SUCCESS = "Success";

    @RequestMapping("/receiveContext-1")
    @ResponseBody
    public String receiveContextService1() throws InterruptedException {
        return "receiveContext-1";
    }

    @RequestMapping("/receiveContext-0")
    @ResponseBody
    public String receiveContextService0() throws InterruptedException {
        return "receiveContext-0";
    }

    @RequestMapping("/okhttp2-scenario")
    @ResponseBody
    public String okHttpScenario() {
        // Like gateway forward trace header.
        Request request = new Request.Builder().url("http://127.0.0.1:8080/okhttp2-scenario/case/receiveContext-0")
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                Request request = new Request.Builder().url(
                                "http://127.0.0.1:8080/okhttp2-scenario/case/receiveContext-1")
                        .build();
                new OkHttpClient().newCall(request).execute();
            }

        });

        return "Success";
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        // your codes
        return SUCCESS;
    }

}
