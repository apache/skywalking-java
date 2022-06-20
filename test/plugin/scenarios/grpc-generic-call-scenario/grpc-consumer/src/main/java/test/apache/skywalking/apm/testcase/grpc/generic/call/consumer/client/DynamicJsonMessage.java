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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistryLite;

import io.grpc.MethodDescriptor;

public class DynamicJsonMessage {
    public static final String DYNAMIC_SERVICE_SUFFIX = "Dynamic";

    public static final String DYNAMIC_MESSAGE_NAME = "DynamicJsonMessage";

    public static final String DYNAMIC_MESSAGE_DATA_FILED = "data";

    private static final Logger LOG = LoggerFactory.getLogger(DynamicJsonMessage.class);

    private static Descriptors.Descriptor buildJsonMarshallerDescriptor() {

        DescriptorProtos.DescriptorProto.Builder jsonMarshaller = DescriptorProtos.DescriptorProto.newBuilder();
        jsonMarshaller.setName(DYNAMIC_MESSAGE_NAME);
        jsonMarshaller.addFieldBuilder()
                .setName(DYNAMIC_MESSAGE_DATA_FILED)
                .setNumber(1)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);

        DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
                DescriptorProtos.FileDescriptorProto.newBuilder();
        fileDescriptorProtoBuilder.addMessageType(jsonMarshaller);

        DescriptorProtos.FileDescriptorProto fileDescriptorProto = fileDescriptorProtoBuilder.build();
        try {
            Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor
                    .buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0]);
            return fileDescriptor.findMessageTypeByName(DYNAMIC_MESSAGE_NAME);
        } catch (Exception e) {
            LOG.error("buildJsonMarshallerDescriptor error: {}", e.getMessage());
            throw new RuntimeException("buildJsonMarshallerDescriptor error", e);
        }
    }

    public static DynamicMessage buildJsonDynamicMessage(String jsonParam) {
        // build Descriptor and set request param
        Descriptors.Descriptor jsonDescriptor = buildJsonMarshallerDescriptor();
        DynamicMessage.Builder jsonDynamicMessage = DynamicMessage.newBuilder(jsonDescriptor);
        jsonDynamicMessage.setField(jsonDescriptor.findFieldByName(DYNAMIC_MESSAGE_DATA_FILED),
                jsonParam);
        return jsonDynamicMessage.build();
    }

    public static DynamicMessage buildJsonDynamicMessage() {
        Descriptors.Descriptor jsonDescriptor = buildJsonMarshallerDescriptor();
        DynamicMessage.Builder jsonDynamicMessage = DynamicMessage.newBuilder(jsonDescriptor);
        return jsonDynamicMessage.build();
    }

    public static String getDataFromDynamicMessage(DynamicMessage message) {
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor key = entry.getKey();
            Object value = entry.getValue();
            String fullName = key.getFullName();
            String jsonMessageFullName = DYNAMIC_MESSAGE_NAME + "." + DYNAMIC_MESSAGE_DATA_FILED;
            if (jsonMessageFullName.equals(fullName)) {
                return (String) value;
            }
        }
        return "";
    }

    public static MethodDescriptor<DynamicMessage, DynamicMessage> createJsonMarshallerMethodDescriptor(
            String serviceName, String methodName, MethodDescriptor.MethodType methodType,
            DynamicMessage request, DynamicMessage response) {

        return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(methodType)
                .setFullMethodName(
                        MethodDescriptor.generateFullMethodName(serviceName + DYNAMIC_SERVICE_SUFFIX,
                                methodName))
                .setRequestMarshaller(new DynamicMessageMarshaller(request.getDescriptorForType()))
                .setResponseMarshaller(new DynamicMessageMarshaller(response.getDescriptorForType()))
                .build();
    }

    private static class DynamicMessageMarshaller implements MethodDescriptor.Marshaller<DynamicMessage> {

        private Descriptors.Descriptor messageDescriptor;

        private DynamicMessageMarshaller(Descriptors.Descriptor messageDescriptor) {
            this.messageDescriptor = messageDescriptor;
        }

        @Override
        public DynamicMessage parse(InputStream inputStream) {
            try {
                return DynamicMessage.newBuilder(messageDescriptor)
                        .mergeFrom(inputStream, ExtensionRegistryLite.getEmptyRegistry())
                        .build();
            } catch (IOException e) {
                throw new RuntimeException("parse inputStream error", e);
            }
        }

        @Override
        public InputStream stream(DynamicMessage abstractMessage) {
            return abstractMessage.toByteString().newInput();
        }
    }
}
