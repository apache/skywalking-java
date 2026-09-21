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
 */

package org.apache.skywalking.apm.plugin.httpclient.v5;

import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

/**
 * The async HttpClient callbacks may run on a thread other than the I/O thread that created the spans, for example
 * the business thread when {@code HttpAsyncClients.classic(...)} is used. Only the span created for the request may
 * be finished by the plugin, and only when it is the active span of the current thread.
 */
public final class OwnedSpans {

    private OwnedSpans() {
    }

    /**
     * @return the local span created for this request if, and only if, it is the active span of the current thread.
     */
    public static AbstractSpan activeOwnedSpan(HttpContext context) {
        if (context == null || !ContextManager.isActive()) {
            return null;
        }
        Object owned = context.getAttribute(Constants.SKYWALKING_LOCAL_SPAN);
        if (owned != null && owned == ContextManager.activeSpan()) {
            return (AbstractSpan) owned;
        }
        return null;
    }
}
