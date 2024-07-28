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
import io.nats.client.impl.NatsMessage;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.PluginException;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.apache.skywalking.apm.plugin.nats.client.NatsCommons.addCommonTag;
import static org.apache.skywalking.apm.plugin.nats.client.NatsCommons.injectCarrier;
import static org.apache.skywalking.apm.plugin.nats.client.NatsCommons.skipTrace;

public class WriterSendMessageBatchInterceptor implements InstanceMethodsAroundInterceptor {

    private static final Field NEXT_FIELD;

    static {
        Field field;
        try {
            field = NatsMessage.class.getDeclaredField("next");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            field = null;
        }
        NEXT_FIELD = field;
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Object next = allArguments[0];
        // set by NatsConnectionWriterConstructorInterceptor
        final String  services = (String) objInst.getSkyWalkingDynamicField();
        while (next != null) {
            if (!skipTrace(next)) {
                Message message = (Message) next;
                EnhancedInstance enhanced = (EnhancedInstance) next;
                AbstractSpan span = ContextManager.createExitSpan("Nats/Pub/" + message.getSubject(), services);
                addCommonTag(span, message);
                Tags.MQ_BROKER.set(span, services);
                Optional.ofNullable(enhanced.getSkyWalkingDynamicField())
                        .ifPresent(snapshot -> ContextManager.continued((ContextSnapshot) snapshot));
                injectCarrier(message);
                //escape from message's lifecycle ahead of time always correct
                enhanced.setSkyWalkingDynamicField(null);
            }
            next = next(next);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        while (ContextManager.isActive()) {
            AbstractSpan abstractSpan = ContextManager.activeSpan();
            ContextManager.stopSpan(abstractSpan);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        while (ContextManager.isActive()) {
            AbstractSpan span = ContextManager.activeSpan();
            span.log(t);
            span.errorOccurred();
            ContextManager.stopSpan(span);
        }
        Object next = allArguments[0];
        try {
            while (next != null) {
                if (!skipTrace(next)) {
                    EnhancedInstance enhanced = (EnhancedInstance) next;
                    //escape from message's lifecycle ahead of time always correct
                    enhanced.setSkyWalkingDynamicField(null);
                }
                next = next(next);
            }
        } catch (IllegalAccessException e) {
            throw new PluginException("nats plugin error", e);
        }
    }

    private NatsMessage next(Object message) throws IllegalAccessException {
        if (NEXT_FIELD == null) {
            return null;
        }
        if (!(message instanceof NatsMessage)) {
            return null;
        }
        return (NatsMessage) NEXT_FIELD.get(message);
    }

}
