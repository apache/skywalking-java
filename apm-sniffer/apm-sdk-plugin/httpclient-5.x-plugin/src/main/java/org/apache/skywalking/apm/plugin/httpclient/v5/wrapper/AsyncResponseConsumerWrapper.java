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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncExitSpan;

public class AsyncResponseConsumerWrapper<T> implements AsyncResponseConsumer<T> {

    private final AsyncResponseConsumer<T> consumer;
    private final AsyncExitSpan exitSpan;

    public AsyncResponseConsumerWrapper(
            AsyncResponseConsumer<T> consumer, AsyncExitSpan exitSpan) {
        this.consumer = consumer;
        this.exitSpan = exitSpan;
    }

    @Override
    public void consumeResponse(
            HttpResponse response,
            EntityDetails entityDetails,
            HttpContext context,
            FutureCallback<T> resultCallback) throws HttpException, IOException {

        exitSpan.onResponse(response.getCode());

        if (entityDetails == null) {
            exitSpan.finish();
        }

        consumer.consumeResponse(response, entityDetails, context, resultCallback);
    }

    @Override
    public void informationResponse(
            HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        consumer.informationResponse(response, context);
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
        exitSpan.finish();
        consumer.streamEnd(trailers);
    }

    @Override
    public void failed(Exception cause) {
        exitSpan.fail(cause);
        consumer.failed(cause);
    }

    @Override
    public void releaseResources() {
        exitSpan.abort();
        consumer.releaseResources();
    }
}