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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import org.apache.hc.core5.http.HttpHost;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

public class AsyncExitSpan {
    private final HttpHost target;
    private volatile Thread creator = Thread.currentThread();
    private AbstractSpan span;

    public AsyncExitSpan(HttpHost target) {
        this.target = target;
    }

    public HttpHost getTarget() {
        return target;
    }

    public boolean claimCreation() {
        if (creator != Thread.currentThread()) {
            return false;
        }
        creator = null;
        return true;
    }

    public void callerReturned() {
        creator = null;
    }

    public synchronized void start(AbstractSpan span) {
        this.span = span;
    }

    public synchronized void onResponse(int statusCode) {
        if (span != null) {
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
            if (statusCode >= 400) {
                span.errorOccurred();
            }
        }
    }

    public synchronized void finish() {
        end(false, null);
    }

    public synchronized void fail(Throwable cause) {
        end(true, cause);
    }

    public synchronized void abort() {
        end(true, null);
    }

    private void end(boolean error, Throwable cause) {
        if (span == null) {
            return;
        }

        if (error) {
            span.errorOccurred();
        }

        if (cause != null) {
            span.log(cause);
        }

        span.asyncFinish();
        span = null;
    }
}
