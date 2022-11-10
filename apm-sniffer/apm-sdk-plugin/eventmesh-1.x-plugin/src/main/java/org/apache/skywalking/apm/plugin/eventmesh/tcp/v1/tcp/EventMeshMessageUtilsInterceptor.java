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
import io.openmessaging.api.Message;
import org.apache.eventmesh.common.protocol.tcp.EventMeshMessage;
import org.apache.eventmesh.common.protocol.tcp.Package;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.plugin.eventmesh.tcp.v1.EventMeshConstants;

import java.lang.reflect.Method;

public class EventMeshMessageUtilsInterceptor implements StaticMethodsAroundInterceptor {

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, MethodInterceptResult result) {
    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Object ret) {
        Object message = allArguments[0];
        Package msg = (Package) ret;

        if (message instanceof CloudEvent) {
            CloudEvent cloudEvent = (CloudEvent) message;
            msg.getHeader().putProperty(EventMeshConstants.CONSTANT_SW_CLOUD_EVENT_HEAD, cloudEvent.getSubject());
        } else if (message instanceof EventMeshMessage) {
            EventMeshMessage eventMeshMessage = (EventMeshMessage) message;
            msg.getHeader().putProperty(EventMeshConstants.CONSTANT_SW_CLOUD_EVENT_HEAD, eventMeshMessage.getTopic());
        } else if (message instanceof Message) {
            Message openMessaging = (Message) message;
            msg.getHeader().putProperty(EventMeshConstants.CONSTANT_SW_CLOUD_EVENT_HEAD, openMessaging.getTopic());
        } else {
            // unsupported protocol for server
            msg.getHeader().putProperty(EventMeshConstants.CONSTANT_SW_CLOUD_EVENT_HEAD, "unSupportMessageType");
        }
        return msg;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
