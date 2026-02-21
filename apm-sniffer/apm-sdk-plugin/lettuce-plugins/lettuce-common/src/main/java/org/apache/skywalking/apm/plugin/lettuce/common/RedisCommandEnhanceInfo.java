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

import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

/**
 * RedisCommandEnhanceInfo holds SkyWalking tracing data for Lettuce commands
 * executed in different asynchronous models.
 *
 * <p>The {@link AbstractSpan} is used for non-reactive (blocking) commands
 * that are executed asynchronously, where the span needs to be created
 * at command submission time and finished when the command completes.</p>
 *
 * <p>The {@link ContextSnapshot} is used for reactive commands, where the
 * tracing context is captured from Reactor {@code Context} and later
 * continued at subscription or execution time to bridge reactive
 * boundaries.</p>
 */
class RedisCommandEnhanceInfo {
    
    private AbstractSpan span;
    private ContextSnapshot snapshot;

    public AbstractSpan getSpan() {
        return span;
    }

    public RedisCommandEnhanceInfo setSpan(AbstractSpan span) {
        this.span = span;
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
