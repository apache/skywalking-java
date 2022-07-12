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
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class AsyncResponseConsumerWrapper<T> implements AsyncResponseConsumer<T> {

    private AsyncResponseConsumer<T> consumer;

    public AsyncResponseConsumerWrapper(AsyncResponseConsumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void consumeResponse(HttpResponse response, EntityDetails entityDetails, HttpContext context,
            FutureCallback<T> resultCallback) throws HttpException, IOException {
        if (ContextManager.isActive()) {
            int statusCode = response.getCode();
            AbstractSpan span = ContextManager.activeSpan();
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
            if (statusCode >= 400) {
                span.errorOccurred();
            }
            ContextManager.stopSpan();
        }
        consumer.consumeResponse(response, entityDetails, context, resultCallback);
    }

    @Override
    public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (ContextManager.isActive()) {
            int statusCode = response.getCode();
            AbstractSpan span = ContextManager.activeSpan();
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
            if (statusCode >= 400) {
                span.errorOccurred();
            }
            ContextManager.stopSpan();
        }
        consumer.informationResponse(response, context);
    }

    @Override
    public void failed(Exception cause) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().errorOccurred().log(cause);
            ContextManager.stopSpan();
        }
        consumer.failed(cause);
    }

    @Override
    public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
        consumer.updateCapacity(capacityChannel);
    }

    @Override
    public void consume(ByteBuffer src) throws IOException {
        consumer.consume(src);
    }

    @Override
    public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
        consumer.streamEnd(trailers);
    }

    @Override
    public void releaseResources() {
        consumer.releaseResources();
    }
}
