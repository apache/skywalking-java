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

package test.apache.skywalking.apm.testcase.grpc.generic.call.consumer.controller;

import java.util.Map;

import test.apache.skywalking.apm.testcase.grpc.generic.call.consumer.client.GrpcGenericCallClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.grpc.MethodDescriptor.MethodType;

@RestController
@RequestMapping("/case")
public class CaseController {

    @Autowired
    private GrpcGenericCallClient grpcGenericCallClient;

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        return "OK";
    }

    @RequestMapping("/generic-call")
    @ResponseBody
    public Object genericCall() throws Exception {
        Thread.sleep(1000);
        Map<String, Object> params = ImmutableMap.of("data", "hello unary");
        return grpcGenericCallClient.genericCall("org.apache.skywalking.apm.testcase.grpc.proto.TestService",
                "unary", MethodType.UNARY, Lists.newArrayList(params));
    }

}
