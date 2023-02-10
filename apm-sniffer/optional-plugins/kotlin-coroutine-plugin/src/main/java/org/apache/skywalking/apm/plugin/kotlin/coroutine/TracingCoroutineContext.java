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
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.jetbrains.annotations.NotNull;

public class TracingCoroutineContext extends AbstractCoroutineContextElement implements ThreadContextElement<ContextSnapshot> {
    private static class Key implements CoroutineContext.Key<TracingCoroutineContext> {
        public static final Key INSTANCE = new Key();

        private Key() {
        }
    }

    private final ContextSnapshot snapshot;

    public TracingCoroutineContext(ContextSnapshot snapshot) {
        super(Key.INSTANCE);
        this.snapshot = snapshot;
    }

    @Override
    public void restoreThreadContext(@NotNull CoroutineContext coroutineContext, ContextSnapshot snapshot) {
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }

        if (snapshot != null) {
            ContextManager.continued(snapshot);
        }
    }

    @Override
    public ContextSnapshot updateThreadContext(@NotNull CoroutineContext coroutineContext) {
        ContextSnapshot old = null;

        if (snapshot != null) {
            if (ContextManager.isActive() && snapshot.isFromCurrent()) {
                return old;
            }

            if (ContextManager.isActive()) {
                old = ContextManager.capture();
            }

            AbstractSpan span = ContextManager.createLocalSpan("Kotlin/Coroutine");
            span.setComponent(ComponentsDefine.KT_COROUTINE);
            // Recover with snapshot
            ContextManager.continued(snapshot);
        }

        return old;
    }
}
