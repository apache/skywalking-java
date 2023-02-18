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

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.IntegerTag;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

import java.util.Optional;

public class NatsCommons {

    private static final String SID = "sid";
    private static final String REPLY_TO = "reply_to";
    private static final String MSG_STATE = "state";
    private static final String MSG = "message";
    private static final String UNKNOWN_SERVER = "unknown_server";

    static boolean skipTrace(Object msg) {
        // include null
        if (!(msg instanceof Message)) {
            return true;
        }
        Message natsMsg = (Message) msg;
        return StringUtil.isBlank(natsMsg.getSubject()) || natsMsg.isStatusMessage();
    }

    static AbstractSpan createEntrySpan(Message message) {
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (StringUtil.isNotEmpty(next.getHeadKey())) {
                next.setHeadValue(message.getHeaders().getFirst(next.getHeadKey()));
            }
        }
        AbstractSpan span = ContextManager.createEntrySpan("Nats/Sub/" + message.getSubject(), contextCarrier);
        addCommonTag(span, message);
        return span;
    }

    static void injectCarrier(Message message) {
        ContextCarrier contextCarrier = new ContextCarrier();
        ContextManager.inject(contextCarrier);
        contextCarrier.extensionInjector().injectSendingTimestamp();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (StringUtil.isNotEmpty(next.getHeadKey())
                && StringUtil.isNotEmpty(next.getHeadValue())) {
                message.getHeaders().add(next.getHeadKey(), next.getHeadValue());
            }
        }
    }

    static void addCommonTag(AbstractSpan span, Message message) {
        Optional.ofNullable(message.getReplyTo()).ifPresent(v -> span.tag(new StringTag(REPLY_TO), v));
        Optional.ofNullable(message.getSID()).ifPresent(v -> span.tag(new StringTag(SID), v));
        Tags.MQ_QUEUE.set(span, message.getSubject());
        span.setComponent(ComponentsDefine.NATS);
        SpanLayer.asMQ(span);
        if (message.getStatus() != null) {
            int code = message.getStatus().getCode();
            String statusMsg = message.getStatus().getMessage();
            span.tag(new IntegerTag(MSG_STATE), String.valueOf(code));
            if (StringUtil.isNotBlank(statusMsg)) {
                span.tag(new StringTag(MSG), statusMsg);
            }
            if (code != 0) {
                span.errorOccurred();
            }
        }
    }

    static MessageHandler buildTraceMsgHandler(String servers, MessageHandler msgHandler) {
        if (msgHandler == null) {
            return null;
        }
        return msg -> {
            if (skipTrace(msg) || msg.getHeaders() == null) {
                msgHandler.onMessage(msg);
                return;
            }
            AbstractSpan span = NatsCommons.createEntrySpan(msg);
            Tags.MQ_BROKER.set(span, servers);
            span.setPeer(servers);
            try {
                msgHandler.onMessage(msg);
            } catch (Exception e) {
                span.log(e).errorOccurred();
                throw e;
            } finally {
                ContextManager.stopSpan(span);
            }
        };

    }

    static String buildServers(Connection connection) {
        return connection.getServers().stream().reduce((a, b) -> a + "," + b).orElse(UNKNOWN_SERVER);
    }

}
