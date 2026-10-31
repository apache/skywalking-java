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

package org.apache.skywalking.apm.plugin.lettuce.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.status.StatusCheckService;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

/**
 * The exit span of one write to Redis, shared by all commands written in it. A command written alone has a span of its
 * own, while the commands written as a batch share one span. The span is finished when the last of its commands is
 * finished, and is an error when any of them fails. Only the first failure is logged, so a batch that fails as a
 * whole, such as by a closed connection or by timeouts, does not log one error per command.
 *
 * <p>Each command finishes its share once, after taking it by {@link RedisCommandEnhanceInfo#takeWriteSpan()}, so a
 * command finished by more than one caller, such as cancelled while it completes, is counted once. The commands of a
 * batch can be finished in different threads, such as by the event loop and by a thread that cancels one of them, so
 * the span is only changed while holding the lock of this object.</p>
 */
class RedisWriteSpan {

    private static final StringTag CANCEL_SIGNAL_TAG = new StringTag("signalType");
    private static final String COMMAND_CANCEL_VALUE = "cancel";

    private final AbstractSpan span;
    /**
     * The shares of the commands, and of the writer that creates the span, which keeps the span unfinished until every
     * command of the write has its share.
     */
    private int unfinishedCommands = 1;
    private boolean failureLogged;

    RedisWriteSpan(AbstractSpan span) {
        this.span = span;
    }

    synchronized void addCommand() {
        unfinishedCommands++;
    }

    synchronized void setPeer(String peer) {
        if (!isFinished()) {
            span.setPeer(peer);
        }
    }

    /**
     * A command failed with an exception, such as a closed connection or a timeout. An exception that is not the first
     * failure is not logged, but still makes the span an error, unless it is one of the ignored exceptions.
     */
    synchronized void log(Throwable t) {
        if (isFirstFailure()) {
            span.log(t);
        } else if (!isFinished() && ServiceManager.INSTANCE.findService(StatusCheckService.class).isError(t)) {
            span.errorOccurred();
        }
    }

    /**
     * A command got an error reply from Redis, such as WRONGTYPE, and was completed without an exception, as a
     * reactive command is.
     */
    synchronized void errorReply(String error) {
        if (isFinished()) {
            return;
        }
        span.errorOccurred();
        if (isFirstFailure()) {
            Map<String, String> event = new LinkedHashMap<>();
            event.put("event", "error");
            event.put("message", error);
            span.log(System.currentTimeMillis(), event);
        }
    }

    synchronized void cancelled() {
        if (isFinished()) {
            return;
        }
        span.errorOccurred();
        if (isFirstFailure()) {
            span.tag(CANCEL_SIGNAL_TAG, COMMAND_CANCEL_VALUE);
        }
    }

    synchronized void errorOccurred() {
        if (!isFinished()) {
            span.errorOccurred();
        }
    }

    /**
     * Finishes the share of one command, and the span once no command of it is unfinished.
     */
    synchronized void finishCommand() {
        if (!isFinished() && --unfinishedCommands == 0) {
            span.asyncFinish();
        }
    }

    private boolean isFinished() {
        return unfinishedCommands == 0;
    }

    private boolean isFirstFailure() {
        if (failureLogged || isFinished()) {
            return false;
        }
        failureLogged = true;
        return true;
    }
}
