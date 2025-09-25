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

package org.apache.skywalking.apm.plugin.jetty.v90.client;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import java.nio.ByteBuffer;

public class ResponseListenerWrapper implements Response.Listener {

    private final Response.Listener callback;

    private final ContextSnapshot context;

    public ResponseListenerWrapper(Response.Listener callback, ContextSnapshot context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    public void onComplete(Result result) {
        AbstractSpan span = ContextManager.createLocalSpan(Constants.PLUGIN_NAME + "/CompleteListener/onComplete");
        span.setComponent(ComponentsDefine.JETTY_CLIENT);
        SpanLayer.asHttp(span);
        if (context != null) {
            ContextManager.continued(context);
        }
        if (callback != null) {
            callback.onComplete(result);
        }
        ContextManager.stopSpan();
    }

    @Override
    public void onHeaders(Response response) {
        callback.onHeaders(response);
    }

    @Override
    public void onContent(Response response, ByteBuffer content) {
        callback.onContent(response, content);
    }

    @Override
    public void onBegin(Response response) {
        callback.onBegin(response);
    }

    @Override
    public boolean onHeader(Response response, HttpField field) {
        return callback.onHeader(response, field);
    }

    @Override
    public void onSuccess(Response response) {
        callback.onSuccess(response);
    }

    @Override
    public void onFailure(Response response, Throwable failure) {
        callback.onFailure(response, failure);
    }
}