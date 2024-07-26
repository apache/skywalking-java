/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.nats.client;

import io.nats.client.Message;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.apache.skywalking.apm.plugin.nats.client.NatsCommons.addCommonTag;
import static org.apache.skywalking.apm.plugin.nats.client.NatsCommons.skipTrace;

public class WriterQueueInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        if (skipTrace(allArguments[0])) {
            return;
        }
        Message message = (Message) allArguments[0];
        EnhancedInstance enhancedMsg = (EnhancedInstance) allArguments[0];
        AbstractSpan span = ContextManager.createLocalSpan("Nats/Pub/Enqueue/" + message.getSubject());
        addCommonTag(span, message);
        enhancedMsg.setSkyWalkingDynamicField(ContextManager.capture());
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (skipTrace(allArguments[0])) {
            return ret;
        }
        AbstractSpan span = ContextManager.activeSpan();
        if (!(Boolean) ret) {
            Map<String, String> eventMap = new HashMap<String, String>();
            eventMap.put("enqueue", "failed");
            span.errorOccurred().log(System.currentTimeMillis(), eventMap);
        }
        ContextManager.stopSpan(span);
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
