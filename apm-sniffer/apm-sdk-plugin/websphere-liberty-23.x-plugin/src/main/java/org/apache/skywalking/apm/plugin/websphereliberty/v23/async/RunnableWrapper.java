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

package org.apache.skywalking.apm.plugin.websphereliberty.v23.async;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

public class RunnableWrapper implements Runnable {

    private final Runnable runnable;

    private final ContextSnapshot snapshot;

    private final AbstractSpan asyncSpan;

    private final String operationName;

    public RunnableWrapper(Runnable runnable, ContextSnapshot snapshot, AbstractSpan asyncSpan, String operationName) {
        this.runnable = runnable;
        this.snapshot = snapshot;
        this.asyncSpan = asyncSpan;
        this.operationName = operationName;
    }

    @Override
    public void run() {
        AbstractSpan span = ContextManager.createLocalSpan(operationName);
        span.setComponent(ComponentsDefine.JDK_THREADING);
        SpanLayer.asHttp(span);

        try {
            ContextManager.continued(snapshot);
            runnable.run();
        } finally {
            ContextManager.stopSpan();
            asyncSpan.asyncFinish();
        }
    }
}
