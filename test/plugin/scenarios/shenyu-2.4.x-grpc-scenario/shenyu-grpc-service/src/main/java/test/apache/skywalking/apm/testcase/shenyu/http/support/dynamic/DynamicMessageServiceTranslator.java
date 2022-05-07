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

package test.apache.skywalking.apm.testcase.shenyu.http.support.dynamic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.protobuf.DynamicMessage;

import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.PrototypeMarshaller;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import test.apache.skywalking.apm.testcase.shenyu.http.support.constant.Constants;

public class DynamicMessageServiceTranslator {

    private static final Map<String, Class<?>> REQUEST_PARAMS_CLASS_MAP = new HashMap<>();

    public static ServerServiceDefinition buildDynamicServerServiceDefinition(ServerServiceDefinition serviceDef)
            throws Exception {
        MethodDescriptor.Marshaller<DynamicMessage> marshaller =
                io.grpc.protobuf.ProtoUtils.marshaller(DynamicJsonMessage.buildJsonDynamicMessage());
        List<ServerMethodDefinition<?, ?>> wrappedMethods = new ArrayList<>();
        List<MethodDescriptor<?, ?>> wrappedDescriptors = new ArrayList<>();

        for (ServerMethodDefinition<?, ?> definition : serviceDef.getMethods()) {
            MethodDescriptor.Marshaller<?> requestMarshaller = definition.getMethodDescriptor().getRequestMarshaller();

            String fullMethodName = definition.getMethodDescriptor().getFullMethodName();

            String[] splitMethodName = fullMethodName.split("/");
            fullMethodName = splitMethodName[0] + Constants.DYNAMIC_SERVICE_SUFFIX + "/" + splitMethodName[1];
            if (requestMarshaller instanceof MethodDescriptor.PrototypeMarshaller) {
                PrototypeMarshaller<?> prototypeMarshaller =
                        (PrototypeMarshaller<?>) requestMarshaller;
                REQUEST_PARAMS_CLASS_MAP.put(fullMethodName, prototypeMarshaller.getMessagePrototype().getClass());
            }
            MethodDescriptor<?, ?> originalMethodDescriptor = definition.getMethodDescriptor();
            MethodDescriptor<DynamicMessage, DynamicMessage> wrappedMethodDescriptor = originalMethodDescriptor
                    .toBuilder(marshaller, marshaller).build();
            wrappedDescriptors.add(wrappedMethodDescriptor);
            wrappedMethods.add(wrapMethod(definition, wrappedMethodDescriptor));
        }

        ServiceDescriptor.Builder build = ServiceDescriptor.newBuilder(
                serviceDef.getServiceDescriptor().getName() + Constants.DYNAMIC_SERVICE_SUFFIX);
        for (MethodDescriptor<?, ?> md : wrappedDescriptors) {
            Field fullMethodNameField = getField(md.getClass(), "fullMethodName");
            fullMethodNameField.setAccessible(true);
            String fullMethodName = (String) fullMethodNameField.get(md);
            String[] splitMethodName = fullMethodName.split("/");
            fullMethodName = splitMethodName[0] + Constants.DYNAMIC_SERVICE_SUFFIX + "/" + splitMethodName[1];
            fullMethodNameField.set(md, fullMethodName);

            String serviceName;
            Field serviceNameField = getField(md.getClass(), "serviceName");
            if (Objects.nonNull(serviceNameField)) {
                serviceNameField.setAccessible(true);
                serviceName = (String) serviceNameField.get(md);
                serviceName = serviceName + Constants.DYNAMIC_SERVICE_SUFFIX;
                serviceNameField.set(md, serviceName);
            }
            build.addMethod(md);
        }
        ServerServiceDefinition.Builder serviceBuilder = ServerServiceDefinition.builder(build.build());

        for (ServerMethodDefinition<?, ?> definition : wrappedMethods) {
            serviceBuilder.addMethod(definition);
        }
        return serviceBuilder.build();
    }

    private static Field getField(final Class<?> beanClass, final String name) throws SecurityException {
        final Field[] fields = beanClass.getDeclaredFields();
        return Arrays.stream(fields).filter(field -> Objects.equals(name, field.getName()))
                .findFirst().orElse(null);
    }

    private static <R, P, W, M> ServerMethodDefinition<W, M> wrapMethod(
            ServerMethodDefinition<R, P> definition,
            MethodDescriptor<W, M> wrappedMethod) {
        ServerCallHandler<W, M> wrappedHandler = wrapHandler(definition.getServerCallHandler());
        return ServerMethodDefinition.create(wrappedMethod, wrappedHandler);
    }

    @SuppressWarnings("unchecked")
    private static <R, P, W, M> ServerCallHandler<W, M> wrapHandler(
            ServerCallHandler<R, P> originalHandler) {
        return (call, headers) -> {
            ServerCall<R, P> unwrappedCall = new ProtoToDynamicServerCall<>((ServerCall<R, P>) call);
            ServerCall.Listener<R> originalListener = originalHandler.startCall(unwrappedCall, headers);
            return new DynamicToProtoServerCallListener(originalListener, unwrappedCall);
        };
    }

    public static Map<String, Class<?>> getRequestParamsClassMap() {
        return REQUEST_PARAMS_CLASS_MAP;
    }

}
