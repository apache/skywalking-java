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

package org.apache.skywalking.apm.plugin.kotlin.coroutine;

import kotlin.coroutines.AbstractCoroutineContextElement;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.ThreadContextElement;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.jetbrains.annotations.NotNull;

public class TracingCoroutineContext extends AbstractCoroutineContextElement implements ThreadContextElement<AbstractSpan> {
    private static class Key implements CoroutineContext.Key<TracingCoroutineContext> {
        public static final Key INSTANCE = new Key();

        private Key() {
        }
    }

    private static final String COROUTINE_OPERATION = "Kotlin/Coroutine";

    private static final ILog LOG = LogManager.getLogger(TracingCoroutineContext.class);

    private final ContextSnapshot snapshot;

    public TracingCoroutineContext(ContextSnapshot snapshot) {
        super(Key.INSTANCE);
        this.snapshot = snapshot;
    }

    @Override
    public void restoreThreadContext(@NotNull CoroutineContext coroutineContext, AbstractSpan span) {
        if (ContextManager.isActive() && span != null) {
            ContextManager.stopSpan(span);
        }
    }

    @Override
    public AbstractSpan updateThreadContext(@NotNull CoroutineContext coroutineContext) {
        // Coroutine will be executed in a new thread, we need recover our tracing context in this thread.

        // Snapshot is null means tracing is disabled in current coroutine.
        if (snapshot != null) {

            // Tracing is enabled on the target dispatched thread.
            if (ContextManager.isActive()) {

                // If the trace context is from the snapshot, it means that the thread has not been switched,
                // and there is no need to create a cross-thread span.
                // If not, it means that we are scheduled to a dirty thread, we log a warning and give up to
                // create cross-thread span.
                if (!snapshot.isFromCurrent()) {
                    LOG.warn("Kotlin coroutine has been dispatched to a dirty thread which with active span: {}.", ContextManager.getGlobalTraceId());
                }
                return null;
            }

            AbstractSpan span = ContextManager.createLocalSpan(COROUTINE_OPERATION);
            span.setComponent(ComponentsDefine.KT_COROUTINE);
            // Recover with snapshot
            ContextManager.continued(snapshot);
            return span;
        }

        return null;
    }
}
