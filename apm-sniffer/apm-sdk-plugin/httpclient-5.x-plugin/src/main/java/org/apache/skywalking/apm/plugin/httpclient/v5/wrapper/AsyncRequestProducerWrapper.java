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
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.httpclient.v5.AsyncExitSpan;

public class AsyncRequestProducerWrapper implements AsyncRequestProducer {

    private final AsyncRequestProducer producer;
    private final AsyncExitSpan exitSpan;

    public AsyncRequestProducerWrapper(AsyncRequestProducer producer, AsyncExitSpan exitSpan) {
        this.producer = producer;
        this.exitSpan = exitSpan;
    }

    public AsyncExitSpan getExitSpan() {
        return exitSpan;
    }

    @Override
    public void sendRequest(RequestChannel channel, HttpContext context) throws IOException, HttpException {
        producer.sendRequest((request, entityDetails, requestContext) -> {
            if (exitSpan.claimCreation()) {
                try {
                    startExitSpan(request);
                } catch (Throwable ignored) {
                    // Never let tracing instrumentation break the user's HTTP request.
                }
            }

            channel.sendRequest(request, entityDetails, requestContext);
        }, context);
    }

    private void startExitSpan(HttpRequest request) {
        String operationName = request.getRequestUri();
        String remotePeer = exitSpan.getTarget().toHostString();

        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(
                operationName,
                contextCarrier,
                remotePeer
        );

        boolean nested = ContextManager.activeSpan().isExit();

        if (!nested) {
            span.setComponent(ComponentsDefine.HTTP_ASYNC_CLIENT);
            Tags.URL.set(span, request.getRequestUri());
            SpanLayer.asHttp(span);
        }

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            request.setHeader(next.getHeadKey(), next.getHeadValue());
            next = next.next();
        }

        if (!nested) {
            span.prepareForAsync();
        }

        ContextManager.stopSpan(span);

        if (!nested) {
            exitSpan.start(span);
        }
    }

    @Override
    public boolean isRepeatable() {
        return producer.isRepeatable();
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        producer.produce(channel);
    }

    @Override
    public int available() {
        return producer.available();
    }

    @Override
    public void failed(Exception cause) {
        producer.failed(cause);
    }

    @Override
    public void releaseResources() {
        producer.releaseResources();
    }
}