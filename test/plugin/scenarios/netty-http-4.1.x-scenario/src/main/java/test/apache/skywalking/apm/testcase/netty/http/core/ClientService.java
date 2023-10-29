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

package test.apache.skywalking.apm.testcase.netty.http.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import test.apache.skywalking.apm.testcase.netty.http.handler.UserClientHandler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class ClientService {
    public static void start() {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();

            ChannelFuture channelFuture = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                            nioSocketChannel.pipeline().addLast(sslContext.newHandler(nioSocketChannel.alloc()));
                            nioSocketChannel.pipeline().addLast(new HttpClientCodec());
                            nioSocketChannel.pipeline().addLast(new HttpObjectAggregator(512 * 1024));
                            nioSocketChannel.pipeline().addLast(new UserClientHandler());
                        }
                    })
                    .connect(new InetSocketAddress("localhost", 8070)).sync();
            Channel channel = channelFuture.channel();

            URI uri = new URI("/user");
            String jsonBody = "{\"name\":\"david\",\"age\":30}";

            FullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.POST,
                    uri.toASCIIString(),
                    Unpooled.wrappedBuffer(jsonBody.getBytes(StandardCharsets.UTF_8)));

            request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            request.headers().set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());

            channel.writeAndFlush(request);
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
