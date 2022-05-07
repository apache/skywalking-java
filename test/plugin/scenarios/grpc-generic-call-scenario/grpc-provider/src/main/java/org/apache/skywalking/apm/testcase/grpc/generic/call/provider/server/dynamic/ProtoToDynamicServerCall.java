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

package org.apache.skywalking.apm.testcase.grpc.generic.call.provider.server.dynamic;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.ServerCall;
import io.grpc.Status;

public class ProtoToDynamicServerCall<R, P> extends SimpleForwardingServerCall<R, P> {

    protected ProtoToDynamicServerCall(ServerCall<R, P> delegate) {
        super(delegate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendMessage(final P message) {
        try {
            if (message == null) {
                delegate().sendMessage(null);
                return;
            }
            // proto message to dynamicMessage
            String jsonFormat = JsonFormat.printer().includingDefaultValueFields().preservingProtoFieldNames()
                    .print((MessageOrBuilder) message);
            DynamicMessage respMessage = DynamicJsonMessage.buildJsonDynamicMessage(jsonFormat);
            delegate().sendMessage((P) respMessage);
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
        }
    }
}
