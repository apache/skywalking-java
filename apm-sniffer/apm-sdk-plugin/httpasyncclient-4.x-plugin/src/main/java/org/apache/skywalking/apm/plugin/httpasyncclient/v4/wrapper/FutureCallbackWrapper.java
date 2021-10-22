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

package org.apache.skywalking.apm.plugin.httpasyncclient.v4.wrapper;

import org.apache.http.concurrent.FutureCallback;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.plugin.httpasyncclient.v4.Constants;

/**
 * a wrapper for {@link FutureCallback} so we can be notified when the hold response (when one or more request fails the
 * pipeline mode may not callback though we haven't support pipeline) received whether it fails or completed or
 * canceled.
 */
public class FutureCallbackWrapper<T> implements FutureCallback<T> {

    private FutureCallback<T> callback;

    public FutureCallbackWrapper(FutureCallback<T> callback) {
        this.callback = callback;
    }

    @Override
    public void completed(T o) {
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
        if (callback != null) {
            callback.completed(o);
        }
    }

    @Override
    public void failed(Exception e) {
        Constants.HTTP_CONTEXT_LOCAL.remove();
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(e);
            ContextManager.stopSpan();
        }
        if (callback != null) {
            callback.failed(e);
        }
    }

    @Override
    public void cancelled() {
        Constants.HTTP_CONTEXT_LOCAL.remove();
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().errorOccurred();
            ContextManager.stopSpan();
        }
        if (callback != null) {
            callback.cancelled();
        }
    }
}
