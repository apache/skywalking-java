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

import java.util.concurrent.atomic.AtomicReference;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;

/**
 * RedisCommandEnhanceInfo holds SkyWalking tracing data for Lettuce commands
 * executed in different asynchronous models.
 *
 * <p>The {@link RedisWriteSpan} is the exit span of the write that sent the
 * command, which is created when the command is written and finished when
 * the command, and every other command of the same write, completes.</p>
 *
 * <p>The {@link ContextSnapshot} is used for reactive commands, where the
 * tracing context is captured from Reactor {@code Context} and later
 * continued at subscription or execution time to bridge reactive
 * boundaries.</p>
 */
class RedisCommandEnhanceInfo {

    private final AtomicReference<RedisWriteSpan> writeSpan = new AtomicReference<>();
    private ContextSnapshot snapshot;

    public RedisWriteSpan getWriteSpan() {
        return writeSpan.get();
    }

    /**
     * @return the write span of the command, or null if the command has no span. A reactive command only carries the
     * snapshot until it is written, so a command cancelled before that has no span to finish.
     */
    static RedisWriteSpan getWriteSpan(EnhancedInstance command) {
        RedisCommandEnhanceInfo enhanceInfo = (RedisCommandEnhanceInfo) command.getSkyWalkingDynamicField();
        return enhanceInfo == null ? null : enhanceInfo.getWriteSpan();
    }

    /**
     * @return the write span of the command, whose share of this command the caller must finish, or null if the
     * command has no span or another caller has taken it. A command can be cancelled while its write is traced, so more
     * than one caller may try to finish its share, and only the first one gets it.
     */
    static RedisWriteSpan takeWriteSpan(EnhancedInstance command) {
        RedisCommandEnhanceInfo enhanceInfo = (RedisCommandEnhanceInfo) command.getSkyWalkingDynamicField();
        return enhanceInfo == null ? null : enhanceInfo.takeWriteSpan();
    }

    RedisWriteSpan takeWriteSpan() {
        return writeSpan.getAndSet(null);
    }

    public RedisCommandEnhanceInfo setWriteSpan(RedisWriteSpan writeSpan) {
        this.writeSpan.set(writeSpan);
        return this;
    }

    public ContextSnapshot getSnapshot() {
        return snapshot;
    }

    public RedisCommandEnhanceInfo setSnapshot(ContextSnapshot snapshot) {
        this.snapshot = snapshot;
        return this;
    }
}
