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

package test.apache.skywalking.apm.testcase.shenyu.http.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import test.apache.skywalking.apm.testcase.shenyu.http.support.dynamic.DynamicMessageServiceTranslator;

@Configuration
public class ServiceConfiguration {

    @Value("${grpc.port}")
    private int grpcPort;

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server server(EchoServiceImpl testService) throws Exception {
        ServerServiceDefinition serviceDefinition = testService.bindService();
        // For testing purposes, the `GRPC Server Reflection Protocol` is not used here.
        // GRPC Server Reflection Protocol: https://github.com/grpc/grpc/blob/master/doc/server-reflection.md
        // No dependency: io.grpc:grpc-services:${grpcVersion}
        ServerServiceDefinition jsonDefinition =
                DynamicMessageServiceTranslator.buildDynamicServerServiceDefinition(serviceDefinition);
        return ServerBuilder.forPort(grpcPort)
                .addService(serviceDefinition)
                .addService(jsonDefinition)
                .build();
    }

}
