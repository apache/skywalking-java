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

package org.apache.skywalking.apm.plugin.rocketMQ.client.java.v5;

import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.NoopSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

/**
 * {@link PublishingMessageImplInterceptor} injects the exit span of the send, created by {@link
 * MessageSendInterceptor} or {@link MessageSendAsyncInterceptor}, into the {@code PublishingMessageImpl} that the
 * producer copies the message into. {@link MessageGetPropertiesInterceptor} adds it to the user properties of the
 * message that is sent, so the message the application passed keeps every attribute, such as the lite topic and the
 * priority of newer clients. When the trace is ignored, the active span is a {@link NoopSpan} instead of an exit span,
 * and the carrier still carries the correlation context and the sending timestamp.
 */
public class PublishingMessageImplInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        if (!ContextManager.isActive()) {
            return;
        }
        AbstractSpan activeSpan = ContextManager.activeSpan();
        if (!activeSpan.isExit() && !(activeSpan instanceof NoopSpan)) {
            return;
        }
        ContextCarrier contextCarrier = new ContextCarrier();
        ContextManager.inject(contextCarrier);
        contextCarrier.extensionInjector().injectSendingTimestamp();
        objInst.setSkyWalkingDynamicField(contextCarrier);
    }
}
