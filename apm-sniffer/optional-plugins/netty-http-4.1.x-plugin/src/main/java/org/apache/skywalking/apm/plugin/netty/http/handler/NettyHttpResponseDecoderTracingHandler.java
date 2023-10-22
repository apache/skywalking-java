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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.plugin.netty.http.common.AttributeKeys;
import org.apache.skywalking.apm.plugin.netty.http.utils.TypeUtils;

@ChannelHandler.Sharable
public class NettyHttpResponseDecoderTracingHandler extends ChannelInboundHandlerAdapter {

    private static final ILog LOGGER = LogManager.getLogger(NettyHttpResponseDecoderTracingHandler.class);

    private static class SingletonHolder {
        private static final NettyHttpResponseDecoderTracingHandler INSTANCE = new NettyHttpResponseDecoderTracingHandler();
    }

    public static NettyHttpResponseDecoderTracingHandler getInstance() {
        return NettyHttpResponseDecoderTracingHandler.SingletonHolder.INSTANCE;
    }

    private NettyHttpResponseDecoderTracingHandler() {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
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
            AbstractSpan span = channel.attr(AttributeKeys.HTTP_CLIENT_SPAN).getAndSet(null);
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
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // to close span in some case there is no response.
        AbstractSpan span = ctx.channel().attr(AttributeKeys.HTTP_CLIENT_SPAN).getAndSet(null);
        if (span != null) {
            span.asyncFinish();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        AbstractSpan span = ctx.channel().attr(AttributeKeys.HTTP_CLIENT_SPAN).getAndSet(null);
        if (span != null) {
            span.errorOccurred().log(cause);
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, 500);
            span.asyncFinish();
        }
        super.exceptionCaught(ctx, cause);
    }
}
