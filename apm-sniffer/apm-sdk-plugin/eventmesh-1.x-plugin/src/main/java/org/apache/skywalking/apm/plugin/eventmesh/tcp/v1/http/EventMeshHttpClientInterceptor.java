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

package org.apache.skywalking.apm.plugin.eventmesh.tcp.v1.http;

import org.apache.eventmesh.client.http.model.RequestParam;
import org.apache.eventmesh.common.protocol.tcp.Command;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.eventmesh.tcp.v1.EventMeshConstants;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

/**
 * the interceptor for eventMesh http client.
 */
public class EventMeshHttpClientInterceptor implements StaticMethodsAroundInterceptor {

    public static final String OPERATE_NAME_PREFIX = "EventMesh/";
    public static final String PRODUCER_OPERATE_NAME_SUFFIX = "/Producer";
    public static final List<Command> EXCLUDE_COMMAND = new ArrayList<>(16);

    static {
        Collections.addAll(EXCLUDE_COMMAND, HEARTBEAT_REQUEST, HELLO_REQUEST,
                CLIENT_GOODBYE_REQUEST, SERVER_GOODBYE_RESPONSE, SUBSCRIBE_REQUEST, UNSUBSCRIBE_REQUEST,
                LISTEN_REQUEST, REGISTER_REQUEST, UNREGISTER_REQUEST, RECOMMEND_REQUEST, RESPONSE_TO_CLIENT_ACK);
    }

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, MethodInterceptResult result) {

        String remoteAddr = (String) allArguments[2];
        RequestParam requestParam = (RequestParam) allArguments[3];

        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(OPERATE_NAME_PREFIX + "Http" + PRODUCER_OPERATE_NAME_SUFFIX, contextCarrier, remoteAddr);
        span.setComponent(ComponentsDefine.EVENT_MESH);
        span.setLayer(SpanLayer.MQ);
        span.setPeer(remoteAddr);
        Tags.MQ_TOPIC.set(span, requestParam.getHeaders().get(EventMeshConstants.CONSTANT_SW_CLOUD_EVENT_HEAD));
    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Object ret) {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
