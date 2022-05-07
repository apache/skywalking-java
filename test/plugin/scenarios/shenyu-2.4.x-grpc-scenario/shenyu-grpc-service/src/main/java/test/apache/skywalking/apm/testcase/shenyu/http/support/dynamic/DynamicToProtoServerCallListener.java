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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.Status;

public class DynamicToProtoServerCallListener<R, P> extends SimpleForwardingServerCallListener<R> {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicToProtoServerCallListener.class);

    private final ServerCall<R, P> call;

    public DynamicToProtoServerCallListener(final Listener<R> delegate, final ServerCall<R, P> call) {
        super(delegate);
        this.call = call;
    }

    /**
     * dynamicMessage  to proto message
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(final R message) {
        Message.Builder builder;
        Class<?> t =
                DynamicMessageServiceTranslator.getRequestParamsClassMap().get(call.getMethodDescriptor().getFullMethodName());
        try {
            builder = (Message.Builder) invokeStaticMethod(t, "newBuilder");
            // dynamicMessage  to proto message
            String reqData = DynamicJsonMessage.getDataFromDynamicMessage((DynamicMessage) message);
            JsonFormat.parser().ignoringUnknownFields().merge(reqData, builder);
            if (Objects.isNull(builder)) {
                throw new RuntimeException("build json response message is error, newBuilder method is null");
            }

            delegate().onMessage((R) builder.build());
        } catch (Exception e) {
            LOG.error("handle json generic request is error", e);
            throw Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
        }
    }

    public static Object invokeStaticMethod(final Class<?> clazz, final String method) {
        try {
            return MethodUtils.invokeStaticMethod(clazz, method);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
