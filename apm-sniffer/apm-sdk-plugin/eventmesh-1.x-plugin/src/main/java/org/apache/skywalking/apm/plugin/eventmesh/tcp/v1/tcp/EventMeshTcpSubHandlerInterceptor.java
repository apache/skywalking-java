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

package org.apache.skywalking.apm.plugin.eventmesh.tcp.v1.tcp;

import io.cloudevents.CloudEvent;
import io.netty.channel.ChannelHandlerContext;
import org.apache.eventmesh.client.tcp.impl.AbstractEventMeshTCPSubHandler;
import org.apache.eventmesh.common.protocol.tcp.Command;
import org.apache.eventmesh.common.protocol.tcp.EventMeshMessage;
import org.apache.eventmesh.common.protocol.tcp.Package;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.eventmesh.common.protocol.tcp.Command.CLIENT_GOODBYE_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.HEARTBEAT_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.HELLO_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.LISTEN_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.RECOMMEND_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.REDIRECT_TO_CLIENT;
import static org.apache.eventmesh.common.protocol.tcp.Command.REGISTER_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.SERVER_GOODBYE_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.SUBSCRIBE_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.UNREGISTER_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.UNSUBSCRIBE_RESPONSE;

/**
 * eventMesh tcp subscribe handler interceptor.
 */
public class EventMeshTcpSubHandlerInterceptor implements InstanceMethodsAroundInterceptor {

    public static final String OPERATE_NAME_PREFIX = "EventMesh/";
    public static final String CONSUMER_OPERATE_NAME_SUFFIX = "/Consumer";

    public static final List<Command> EXCLUDE_COMMAND = new ArrayList<>(16);

    static {
        Collections.addAll(EXCLUDE_COMMAND, HEARTBEAT_RESPONSE, HELLO_RESPONSE,
                CLIENT_GOODBYE_RESPONSE, SERVER_GOODBYE_REQUEST, SUBSCRIBE_RESPONSE, UNSUBSCRIBE_RESPONSE,
                LISTEN_RESPONSE, REDIRECT_TO_CLIENT, REGISTER_RESPONSE, UNREGISTER_RESPONSE, RECOMMEND_RESPONSE);
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) allArguments[0];
        Package packages = (Package) allArguments[1];
        Command cmd = packages.getHeader().getCmd();
        if (EXCLUDE_COMMAND.contains(cmd)) {
            // ignore this type of request.
            return;
        }

        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createEntrySpan(OPERATE_NAME_PREFIX + cmd.name() + CONSUMER_OPERATE_NAME_SUFFIX, contextCarrier);
        SpanLayer.asMQ(span);
        span.setComponent(ComponentsDefine.EVENT_MESH_CONSUMER);

        String remoteServerAddress = channelHandlerContext.channel().remoteAddress().toString();
        if (remoteServerAddress.startsWith("/")) {
            remoteServerAddress = remoteServerAddress.substring(1);
        }
        Tags.MQ_BROKER.set(span, remoteServerAddress);
        Object protocolMessage = ((AbstractEventMeshTCPSubHandler) objInst).getProtocolMessage(packages);
        if (protocolMessage instanceof CloudEvent) {
            Tags.MQ_QUEUE.set(span, ((CloudEvent) protocolMessage).getSubject());
        } else if (protocolMessage instanceof EventMeshMessage) {
            Tags.MQ_QUEUE.set(span, ((EventMeshMessage) protocolMessage).getTopic());
        }

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }

}
