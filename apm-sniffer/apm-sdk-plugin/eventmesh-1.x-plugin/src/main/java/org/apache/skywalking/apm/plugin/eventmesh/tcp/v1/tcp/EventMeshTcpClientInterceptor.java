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

import org.apache.eventmesh.client.tcp.conf.EventMeshTCPClientConfig;
import org.apache.eventmesh.common.protocol.tcp.Command;
import org.apache.eventmesh.common.protocol.tcp.OPStatus;
import org.apache.eventmesh.common.protocol.tcp.Package;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.eventmesh.common.protocol.tcp.Command.CLIENT_GOODBYE_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.HEARTBEAT_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.HELLO_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.LISTEN_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.RECOMMEND_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.REGISTER_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.RESPONSE_TO_CLIENT_ACK;
import static org.apache.eventmesh.common.protocol.tcp.Command.SERVER_GOODBYE_RESPONSE;
import static org.apache.eventmesh.common.protocol.tcp.Command.SUBSCRIBE_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.UNREGISTER_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.UNSUBSCRIBE_REQUEST;

public class EventMeshTcpClientInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    public static final String OPERATE_NAME_PREFIX = "EventMesh/";
    public static final String PRODUCER_OPERATE_NAME_SUFFIX = "/Producer";
    public static final List<Command> EXCLUDE_COMMAND = new ArrayList<>(16);

    static {
        Collections.addAll(EXCLUDE_COMMAND, HEARTBEAT_REQUEST, HELLO_REQUEST,
                CLIENT_GOODBYE_REQUEST, SERVER_GOODBYE_RESPONSE, SUBSCRIBE_REQUEST, UNSUBSCRIBE_REQUEST,
                LISTEN_REQUEST, REGISTER_REQUEST, UNREGISTER_REQUEST, RECOMMEND_REQUEST, RESPONSE_TO_CLIENT_ACK);
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Package packages = (Package) allArguments[0];
        Command cmd = packages.getHeader().getCmd();
        if (EXCLUDE_COMMAND.contains(cmd)) {
            // ignore this type of request.
            return;
        }
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(OPERATE_NAME_PREFIX + cmd.name() + PRODUCER_OPERATE_NAME_SUFFIX, contextCarrier, (String) objInst.getSkyWalkingDynamicField());
        span.setLayer(SpanLayer.MQ);
        span.setComponent(ComponentsDefine.EVENT_MESH);
        Tags.URL.set(span, (String) objInst.getSkyWalkingDynamicField());
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        Package packages = (Package) allArguments[0];
        AbstractSpan activeSpan = ContextManager.activeSpan();
        Tags.MQ_STATUS.set(activeSpan, statue(packages.getHeader().getCode()));
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) throws Throwable {
        EventMeshTCPClientConfig eventMeshTCPClientConfig = (EventMeshTCPClientConfig) allArguments[0];
        String host = eventMeshTCPClientConfig.getHost();
        int port = eventMeshTCPClientConfig.getPort();
        objInst.setSkyWalkingDynamicField(host + ":" + port);
    }

    public String statue(int code) {
        for (OPStatus status : OPStatus.values()) {
            if (Objects.equals(status.getCode(), code)) {
                return status.name();
            }
        }
        return "Unknown";
    }

}
