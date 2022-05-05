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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

import io.grpc.stub.StreamObserver;

public class SimpleStreamObserver<T extends Message> implements StreamObserver<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleStreamObserver.class);

    private final List<String> responseDataList = new ArrayList<>();

    private final CompletableFuture<Void> completableFuture = new CompletableFuture<>();

    @Override
    public void onNext(T value) {
        try {
            // dynamicMessage to json
            String respData = DynamicJsonMessage.getDataFromDynamicMessage((DynamicMessage) value);
            responseDataList.add(respData);
        } catch (Exception e) {
            LOG.error("parse error", e);
        }
    }

    @Override
    public void onError(Throwable t) {
        LOG.error("SimpleStreamObserver onError", t);
        completableFuture.completeExceptionally(t);
    }

    @Override
    public void onCompleted() {
        LOG.info("SimpleStreamObserver onCompleted");
        completableFuture.complete(null);
    }

    public List<Object> syncGetResponseDataList() throws Exception {
        completableFuture.get();
        return responseDataList.stream().map(JSON::parseObject).collect(Collectors.toList());
    }

    public CompletableFuture<Void> getCompletableFuture() {
        return completableFuture;
    }
}
