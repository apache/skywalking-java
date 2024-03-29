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

package org.apache.skywalking.apm.plugin.netty.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.netty.http.common.AttributeKeys;
import org.apache.skywalking.apm.plugin.netty.http.handler.NettyHttpRequestDecoderTracingHandler;

import java.lang.reflect.Method;
import java.util.Map;

public class AddHttpRequestDecoderInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ChannelHandlerContext ctx = (ChannelHandlerContext) allArguments[0];
        ChannelPipeline pipeline = ctx.channel().pipeline();

        if (pipeline.context(ctx.name()) == null) {
            return ret;
        }

        String name = NettyHttpRequestDecoderTracingHandler.class.getName();
        if (null != pipeline.context(name)) {
            pipeline.remove(name);
        }
        pipeline.addAfter(ctx.name(), name, NettyHttpRequestDecoderTracingHandler.getInstance());

        Map<ChannelHandler, ChannelHandler> map = AttributeKeys.getOrCreateHandlerMap(ctx.channel());
        map.put((ChannelHandler) objInst, NettyHttpRequestDecoderTracingHandler.getInstance());
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
