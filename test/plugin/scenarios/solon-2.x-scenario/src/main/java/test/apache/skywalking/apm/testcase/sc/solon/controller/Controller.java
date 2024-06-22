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

package test.apache.skywalking.apm.testcase.sc.solon.controller;

import cn.hutool.http.HttpRequest;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Path;
import test.apache.skywalking.apm.testcase.sc.solon.service.TestService;

import java.sql.SQLException;

@org.noear.solon.annotation.Controller
public class Controller {

    @Inject
    private TestService testService;

    @Mapping("/testcase/healthCheck")
    public String healthCheck() {
        return "healthCheck";
    }

    @Get
    @Mapping("/testcase/{test}")
    public String hello(@Body String body, @Path("test") String test) throws SQLException {
        testService.executeSQL();
        String body1 = HttpRequest.get("http://localhost:8082/testcase/error").execute().body();
        return "Hello World";
    }

    @Get
    @Mapping("/testcase/error")
    public String error() {
        throw new RuntimeException("this is Error");
    }

}
