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

package org.apache.skywalking.apm.testcase.grpc.generic.call.provider.provider.service;

import org.apache.skywalking.apm.testcase.grpc.proto.RequestData;
import org.apache.skywalking.apm.testcase.grpc.proto.ResponseData;
import org.apache.skywalking.apm.testcase.grpc.proto.TestServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.grpc.stub.StreamObserver;

@Service
public class TestServiceImpl extends TestServiceGrpc.TestServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(TestServiceImpl.class);

    @Override
    public void unary(RequestData request, StreamObserver<ResponseData> responseObserver) {
        LOG.info("server unary receive data:{}", request.getData());
        ResponseData responseData = ResponseData.newBuilder().setData("unaryFun response").build();
        responseObserver.onNext(responseData);
        responseObserver.onCompleted();
    }

    @Override
    public void serverStreaming(RequestData request, StreamObserver<ResponseData> responseObserver) {
        LOG.info("server serverStreaming receive data:{}", request.getData());
        for (int i = 0; i < 2; i++) {
            ResponseData responseData = ResponseData.newBuilder().setData("serverStreaming response data " + i).build();
            responseObserver.onNext(responseData);
        }
        responseObserver.onCompleted();
    }

}
