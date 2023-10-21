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
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsciiString;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.netty.http.common.AttributeKeys;
import org.apache.skywalking.apm.plugin.netty.http.common.NettyConstants;
import org.apache.skywalking.apm.plugin.netty.http.utils.HttpDataCollectUtils;
import org.apache.skywalking.apm.plugin.netty.http.utils.TypeUtils;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class NettyHttpRequestEncoderTracingHandler extends ChannelOutboundHandlerAdapter {

    private static class SingletonHolder {
        private static final NettyHttpRequestEncoderTracingHandler INSTANCE = new NettyHttpRequestEncoderTracingHandler();
    }

    public static NettyHttpRequestEncoderTracingHandler getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private NettyHttpRequestEncoderTracingHandler() {

    }

    private static final ILog LOGGER = LogManager.getLogger(NettyHttpRequestEncoderTracingHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try {
            if (!TypeUtils.isHttpRequest(msg)) {
                return;
            }

            AbstractSpan lastSpan = ctx.channel().attr(AttributeKeys.HTTP_CLIENT_SPAN).getAndSet(null);
            if (null != lastSpan) {
                ContextManager.stopSpan(lastSpan);
            }

            HttpRequest request = (HttpRequest) msg;
            HttpHeaders headers = request.headers();
            String uri = request.uri();
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            String peer = address.getHostString() + ":" + address.getPort();
            String url = peer + uri;
            String method = request.method().toString();

            ContextCarrier contextCarrier = new ContextCarrier();
            AbstractSpan span = ContextManager.createExitSpan(method + ":" + uri, contextCarrier, peer);

            for (CarrierItem item = contextCarrier.items(); item.hasNext(); ) {
                item = item.next();
                headers.add(AsciiString.of(item.getHeadKey()), item.getHeadValue());
            }

            SpanLayer.asHttp(span);
            span.setPeer(peer);
            span.setComponent(ComponentsDefine.NETTY_HTTP);

            boolean sslFlag = ctx.channel().pipeline().context(SslHandler.class) != null;
            Tags.URL.set(span, sslFlag ? NettyConstants.HTTPS_PROTOCOL_PREFIX + url : NettyConstants.HTTP_PROTOCOL_PREFIX + url);
            Tags.HTTP.METHOD.set(span, request.method().name());

            HttpDataCollectUtils.collectHttpRequestBody(request.headers(), ((FullHttpRequest) request).content(), span);

            ctx.channel().attr(AttributeKeys.HTTP_CLIENT_SPAN).set(span);
        } catch (Exception e) {
            LOGGER.error("Fail to trace netty http request", e);
        } finally {
            try {
                ctx.write(msg, promise);
            } catch (Throwable throwable) {
                AbstractSpan span = ctx.channel().attr(AttributeKeys.HTTP_CLIENT_SPAN).getAndSet(null);
                if (span != null) {
                    span.errorOccurred();
                    span.log(throwable);
                    Tags.HTTP_RESPONSE_STATUS_CODE.set(span, 500);
                    ContextManager.stopSpan(span);
                }
                throw throwable;
            }
        }
    }
}
