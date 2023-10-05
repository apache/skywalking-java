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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.netty.constant.NettyConstants;
import org.apache.skywalking.apm.plugin.netty.utils.TypeUtils;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class NettyHttpRequestDecoderTracingHandler extends ChannelInboundHandlerAdapter {

    private static final ILog LOGGER = LogManager.getLogger(NettyHttpRequestDecoderTracingHandler.class);

    private static class SingletonHolder {
        private static final NettyHttpRequestDecoderTracingHandler INSTANCE = new NettyHttpRequestDecoderTracingHandler();
    }

    public static NettyHttpRequestDecoderTracingHandler getInstance() {
        return NettyHttpRequestDecoderTracingHandler.SingletonHolder.INSTANCE;
    }

    private NettyHttpRequestDecoderTracingHandler() {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        AbstractSpan span = null;
        try {
            if (!TypeUtils.isHttpRequest(msg)) {
                return;
            }

            HttpRequest request = (HttpRequest) msg;

            AbstractSpan lastSpan = ctx.channel().attr(NettyConstants.HTTP_SERVER_SPAN).getAndSet(null);
            if (null != lastSpan) {
                ContextManager.stopSpan(lastSpan);
            }

            HttpHeaders headers = request.headers();

            ContextCarrier contextCarrier = new ContextCarrier();
            for (CarrierItem item = contextCarrier.items(); item.hasNext(); ) {
                item = item.next();
                item.setHeadValue(headers.get(item.getHeadKey()));
            }

            InetSocketAddress address = (InetSocketAddress) ctx.channel().localAddress();
            String peer = address.getAddress().getHostAddress() + ":" + address.getPort();
            String url = peer + request.uri();
            String method = request.method().name();

            span = ContextManager.createEntrySpan(method + ":" + request.uri(), contextCarrier);

            SpanLayer.asHttp(span);
            span.setComponent(ComponentsDefine.NETTY);
            Tags.HTTP.METHOD.set(span, method);
            Tags.URL.set(span, NettyConstants.HTTP_PROTOCOL_PREFIX + url);
            ctx.channel().attr(NettyConstants.HTTP_SERVER_SPAN).set(span);
        } catch (Exception e) {
            LOGGER.error("Fail to trace netty http request", e);
        } finally {
            try {
                ctx.fireChannelRead(msg);
            } catch (Throwable throwable) {
                if (span != null) {
                    span.errorOccurred();
                    span.log(throwable);
                    Tags.HTTP_RESPONSE_STATUS_CODE.set(span, 500);
                }
                ContextManager.stopSpan(span);
                throw throwable;
            }
        }
    }
}
