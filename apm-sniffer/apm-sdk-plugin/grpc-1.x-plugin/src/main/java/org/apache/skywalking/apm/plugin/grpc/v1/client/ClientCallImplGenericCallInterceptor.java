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

package org.apache.skywalking.apm.plugin.grpc.v1.client;

import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.CLIENT;
import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.CLIENT_STREAM_PEER;
import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.GENERIC_CALL_METHOD;
import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.RESPONSE_ON_MESSAGE_OPERATION_NAME;
import static org.apache.skywalking.apm.plugin.grpc.v1.OperationNameFormatUtil.formatOperationName;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.ExitSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.grpc.v1.OperationNameFormatUtil;
import org.apache.skywalking.apm.util.StringUtil;

import com.google.common.base.Strings;

import io.grpc.Attributes;
import io.grpc.ClientCall.Listener;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientCallImplGenericCallInterceptor
        implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) throws Throwable {
        MethodDescriptor methodDescriptor = (MethodDescriptor) allArguments[0];
        objInst.setSkyWalkingDynamicField(methodDescriptor);
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            MethodInterceptResult result) throws Throwable {
        String asyncCallMethod = (String) ContextManager.getRuntimeContext().get(GENERIC_CALL_METHOD);
        // only trace generic call
        if (StringUtil.isBlank(asyncCallMethod)) {
            return;
        }
        ContextManager.getRuntimeContext().remove(GENERIC_CALL_METHOD);

        Listener<?> observer = (Listener<?>) allArguments[0];
        Metadata headers = (Metadata) allArguments[1];
        MethodDescriptor methodDescriptor = (MethodDescriptor) objInst.getSkyWalkingDynamicField();
        String serviceName = formatOperationName(methodDescriptor);
        // channel.authority() In some scenes, it is not accurate. eg:Load balancing, NameResolver
        // The server IP and PORT can be obtained accurately BY clientStream.
        // afterMethod method will set remotePeer.
        String remotePeer = "No Peer";
        String operationPrefix = OperationNameFormatUtil.formatOperationName(methodDescriptor) + CLIENT;

        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(serviceName, contextCarrier, remotePeer);
        span.setComponent(ComponentsDefine.GRPC);
        span.setLayer(SpanLayer.RPC_FRAMEWORK);
        span.tag(Tags.ofKey(GENERIC_CALL_METHOD), asyncCallMethod);

        CarrierItem contextItem = contextCarrier.items();
        while (contextItem.hasNext()) {
            contextItem = contextItem.next();
            Metadata.Key<String> headerKey =
                    Metadata.Key.of(contextItem.getHeadKey(), Metadata.ASCII_STRING_MARSHALLER);
            headers.put(headerKey, contextItem.getHeadValue());
        }
        ContextSnapshot snapshot = ContextManager.capture();
        span.prepareForAsync();
        ContextManager.stopSpan(span);
        objInst.setSkyWalkingDynamicField(span);

        allArguments[0] = new TracingClientCallListener<>(observer, methodDescriptor, operationPrefix, snapshot, span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            Object ret) throws Throwable {
        if (objInst.getSkyWalkingDynamicField() == null
                || !(objInst.getSkyWalkingDynamicField() instanceof AbstractSpan)) {
            return ret;
        }
        AbstractSpan span = (AbstractSpan) objInst.getSkyWalkingDynamicField();
        // Scenario of specifying IP + port
        String remotePeer = (String) ContextManager.getRuntimeContext().get(CLIENT_STREAM_PEER);
        ContextManager.getRuntimeContext().remove(CLIENT_STREAM_PEER);
        span.setPeer(remotePeer);
        Arrays.stream(objInst.getClass().getDeclaredMethods()).filter(m -> m.getName().equals("getAttributes"))
                .findFirst()
                .ifPresent(m -> {
                    try {
                        m.setAccessible(true);
                        Attributes attributes = (Attributes) m.invoke(objInst);
                        attributes.keys().stream()
                                .filter(k -> k.toString().equals("remote-addr")).findFirst()
                                .map(attributes::get)
                                .ifPresent(v -> {
                                    String peer = v.toString();
                                    if (StringUtil.isNotBlank(peer)) {
                                        if (peer.startsWith("/")) {
                                            peer = peer.substring(1);
                                        }
                                        // Accurate IP acquisition.Scenario: Name Resolver , load balancing
                                        span.setPeer(peer);
                                    }
                                });
                    } catch (Exception e) {
                        // ignore
                    }
                });
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = (AbstractSpan) objInst.getSkyWalkingDynamicField();
        if (span != null) {
            span.errorOccurred().log(t);
        }
    }

    static class TracingClientCallListener<RESPONSE>
            extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RESPONSE> {

        private final ContextSnapshot contextSnapshot;

        private final MethodDescriptor<?, ?> methodDescriptor;

        private final String operationPrefix;

        private final AbstractSpan asyncSpan;

        TracingClientCallListener(Listener<RESPONSE> delegate, MethodDescriptor<?, ?> methodDescriptor,
                String operationPrefix, ContextSnapshot contextSnapshot, AbstractSpan asyncSpan) {
            super(delegate);
            this.methodDescriptor = methodDescriptor;
            this.operationPrefix = operationPrefix;
            this.contextSnapshot = contextSnapshot;
            this.asyncSpan = asyncSpan;
        }

        @Override
        public void onMessage(RESPONSE message) {
            if (methodDescriptor.getType().serverSendsOneMessage()) {
                super.onMessage(message);
            } else {
                // tracing SERVER_STREAMING
                final AbstractSpan span = ContextManager
                        .createLocalSpan(operationPrefix + RESPONSE_ON_MESSAGE_OPERATION_NAME);
                span.setComponent(ComponentsDefine.GRPC);
                span.setLayer(SpanLayer.RPC_FRAMEWORK);
                ContextManager.continued(contextSnapshot);
                try {
                    delegate().onMessage(message);
                } catch (Throwable t) {
                    span.log(t);
                    throw t;
                } finally {
                    ContextManager.stopSpan(span);
                }
            }
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            if (!status.isOk()) {
                asyncSpan.log(status.asRuntimeException());
            }
            Tags.RPC_RESPONSE_STATUS_CODE.set(asyncSpan, status.getCode().name());
            try {
                delegate().onClose(status, trailers);
            } catch (Throwable t) {
                asyncSpan.log(t);
                throw t;
            } finally {
                // finish async exitSpan
                if (asyncSpan instanceof ExitSpan
                        && ContextManager.getRuntimeContext().get(CLIENT_STREAM_PEER) != null) {
                    // why need this?  because first grpc call will create PendingStream(Unable to get IP)
                    // Delayed create NettyClientStream.
                    // In this case, the constructor of NettyClientStream and the onClose are executed in the same thread
                    ExitSpan exitSpan = (ExitSpan) asyncSpan;
                    if (Strings.isNullOrEmpty(exitSpan.getPeer())) {
                        asyncSpan.setPeer((String) ContextManager.getRuntimeContext().get(CLIENT_STREAM_PEER));
                        ContextManager.getRuntimeContext().remove(CLIENT_STREAM_PEER);
                    }
                }
                asyncSpan.asyncFinish();
            }
        }
    }
}
