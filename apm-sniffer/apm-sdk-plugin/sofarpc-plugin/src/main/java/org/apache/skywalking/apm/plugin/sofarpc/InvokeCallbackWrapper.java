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

package org.apache.skywalking.apm.plugin.sofarpc;

import com.alipay.remoting.InvokeCallback;
import java.util.concurrent.Executor;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

public class InvokeCallbackWrapper implements InvokeCallback {

    @Getter(AccessLevel.PACKAGE)
    private ContextSnapshot contextSnapshot;
    @Getter(AccessLevel.PACKAGE)
    private final InvokeCallback invokeCallback;

    public InvokeCallbackWrapper(InvokeCallback invokeCallback) {
        if (ContextManager.isActive()) {
            this.contextSnapshot = ContextManager.capture();
        }
        this.invokeCallback = invokeCallback;
    }

    @Override
    public void onResponse(final Object o) {
        ContextManager.createLocalSpan("Thread/" + invokeCallback.getClass().getName() + "/onResponse");
        if (contextSnapshot != null) {
            ContextManager.continued(contextSnapshot);
        }
        try {
            invokeCallback.onResponse(o);
        } catch (Throwable t) {
            ContextManager.activeSpan().log(t);
            throw t;
        } finally {
            contextSnapshot = null;
            ContextManager.stopSpan();
        }

    }

    @Override
    public void onException(final Throwable throwable) {
        ContextManager.createLocalSpan("Thread/" + invokeCallback.getClass().getName() + "/onException");
        if (contextSnapshot != null) {
            ContextManager.continued(contextSnapshot);
        }
        if (throwable != null) {
            AbstractSpan abstractSpan = ContextManager.activeSpan();
            if (abstractSpan != null) {
                abstractSpan.log(throwable);
            }
        }
        try {
            invokeCallback.onException(throwable);
        } catch (Throwable t) {
            ContextManager.activeSpan().log(t);
            throw t;
        } finally {
            contextSnapshot = null;
            ContextManager.stopSpan();
        }
    }

    @Override
    public Executor getExecutor() {
        return invokeCallback.getExecutor();
    }
}
