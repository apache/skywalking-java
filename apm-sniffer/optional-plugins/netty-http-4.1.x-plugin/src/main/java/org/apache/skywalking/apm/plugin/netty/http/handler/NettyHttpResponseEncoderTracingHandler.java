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

package org.apache.skywalking.apm.plugin.netty.http.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.plugin.netty.http.common.AttributeKeys;
import org.apache.skywalking.apm.plugin.netty.http.utils.TypeUtils;

@ChannelHandler.Sharable
public class NettyHttpResponseEncoderTracingHandler extends ChannelOutboundHandlerAdapter {

    private static final ILog LOGGER = LogManager.getLogger(NettyHttpResponseEncoderTracingHandler.class);

    private static class SingletonHolder {
        private static final NettyHttpResponseEncoderTracingHandler INSTANCE = new NettyHttpResponseEncoderTracingHandler();
    }

    public static NettyHttpResponseEncoderTracingHandler getInstance() {
        return NettyHttpResponseEncoderTracingHandler.SingletonHolder.INSTANCE;
    }

    private NettyHttpResponseEncoderTracingHandler() {

    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try {
            if (!TypeUtils.isHttpResponse(msg)) {
                return;
            }

            HttpResponse response = (HttpResponse) msg;
            int code = response.status().code();
            if (HttpResponseStatus.CONTINUE.code() == code) {
                return;
            }

            Channel channel = ctx.channel();
            AbstractSpan span = channel.attr(AttributeKeys.HTTP_SERVER_SPAN).getAndSet(null);
            if (span == null) {
                return;
            }

            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, code);
            if (code >= 400) {
                span.errorOccurred();
            }
            span.asyncFinish();
        } catch (Exception e) {
            LOGGER.error("Fail to trace netty http response", e);
        } finally {
            ctx.write(msg, promise);
        }
    }
}
