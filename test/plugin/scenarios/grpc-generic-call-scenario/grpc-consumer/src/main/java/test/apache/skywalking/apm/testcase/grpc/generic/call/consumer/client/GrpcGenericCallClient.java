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

package test.apache.skywalking.apm.testcase.grpc.generic.call.consumer.client;

import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.DynamicMessage;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;

@Component
public class GrpcGenericCallClient implements InitializingBean {

    @Value("${grpc.port}")
    private int grpcPort;

    private ManagedChannel channel;

    @Override
    public void afterPropertiesSet() throws Exception {
        channel = ManagedChannelBuilder.forAddress("localhost", grpcPort).usePlaintext(true).build();
    }

    /**
     * generic call(without grpc stubs and proto)
     */
    public Object genericCall(String serviceName, String methodName, MethodDescriptor.MethodType methodType,
            List<Map<String, Object>> paramsList) throws Exception {
        // param to dynamicMessage
        List<DynamicMessage> jsonRequestList = paramsList.stream()
                .map(params -> DynamicJsonMessage.buildJsonDynamicMessage(JSON.toJSONString(params))).collect(Collectors.toList());
        MethodDescriptor<DynamicMessage, DynamicMessage> jsonMarshallerMethodDescriptor =
                DynamicJsonMessage.createJsonMarshallerMethodDescriptor(serviceName, methodName, methodType,
                        jsonRequestList.get(0), DynamicJsonMessage.buildJsonDynamicMessage());
        SimpleStreamObserver<DynamicMessage> simpleStreamObserver = new SimpleStreamObserver<>();
        ClientCall<DynamicMessage, DynamicMessage> call = channel.newCall(jsonMarshallerMethodDescriptor,
                CallOptions.DEFAULT.withDeadlineAfter(10000, TimeUnit.MILLISECONDS));

        switch (methodType) {
            case UNARY:
                asyncUnaryCall(call, jsonRequestList.get(0), simpleStreamObserver);
                return simpleStreamObserver.syncGetResponseDataList();
            case SERVER_STREAMING:
                asyncServerStreamingCall(call, jsonRequestList.get(0), simpleStreamObserver);
                return simpleStreamObserver.syncGetResponseDataList();
            default:
                return null;
        }
    }

}
