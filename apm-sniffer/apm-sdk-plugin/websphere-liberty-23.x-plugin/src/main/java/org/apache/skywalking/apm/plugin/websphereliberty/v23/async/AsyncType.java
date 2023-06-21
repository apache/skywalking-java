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
import org.apache.skywalking.apm.plugin.webspheresource.com.ibm.ws.webcontainer.async.CompleteRunnable;
import org.apache.skywalking.apm.plugin.webspheresource.com.ibm.ws.webcontainer.async.DispatchRunnable;

public enum AsyncType {
    START {
        @Override
        public RunnableWrapper wrapper(final Runnable runnable) {
            ContextManager.activeSpan().prepareForAsync();
            return new PropagateRunnableWrapper(
                runnable, ContextManager.capture(), ContextManager.activeSpan(), "WebSphereAsync/start");
        }
    },
    DISPATCH {
        @Override
        public RunnableWrapper wrapper(final Runnable runnable) {
            ContextManager.activeSpan().prepareForAsync();
            return new PropagateRunnableWrapper(
                runnable, ContextManager.capture(), ContextManager.activeSpan(), "WebSphereAsync/dispatch");
        }
    },
    COMPLETE {
        @Override
        public RunnableWrapper wrapper(final Runnable runnable) {
            return new RunnableWrapper(runnable);
        }
    };

    protected abstract RunnableWrapper wrapper(Runnable runnable);

    public static RunnableWrapper doWrap(Runnable runnable) {
        if (runnable instanceof CompleteRunnable) {
            return COMPLETE.wrapper(runnable);
        } else if (runnable instanceof DispatchRunnable) {
            return DISPATCH.wrapper(runnable);
        } else {
            return START.wrapper(runnable);
        }
    }
}
