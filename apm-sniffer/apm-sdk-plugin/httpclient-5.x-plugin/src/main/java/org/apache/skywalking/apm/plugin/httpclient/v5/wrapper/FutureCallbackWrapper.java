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

package org.apache.skywalking.apm.plugin.httpclient.v5.wrapper;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncExitSpan;

public class FutureCallbackWrapper<T> implements FutureCallback<T> {

    private final FutureCallback<T> callback;
    private final AsyncExitSpan exitSpan;

    public FutureCallbackWrapper(FutureCallback<T> callback, AsyncExitSpan exitSpan) {
        this.callback = callback;
        this.exitSpan = exitSpan;
    }

    @Override
    public void completed(T o) {
        exitSpan.finish();

        if (callback != null) {
            callback.completed(o);
        }
    }

    @Override
    public void failed(Exception e) {
        exitSpan.fail(e);

        if (callback != null) {
            callback.failed(e);
        }
    }

    @Override
    public void cancelled() {
        exitSpan.abort();

        if (callback != null) {
            callback.cancelled();
        }
    }
}