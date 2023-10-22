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

package org.apache.skywalking.apm.plugin.netty.http.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.plugin.netty.http.config.NettyHttpPluginConfig;
import org.apache.skywalking.apm.util.StringUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HttpDataCollectUtils {

    private static final ILog LOGGER = LogManager.getLogger(HttpDataCollectUtils.class);

    public static void collectHttpRequestBody(HttpHeaders headers, ByteBuf content, AbstractSpan span) {
        try {
            if (headers == null || content == null || span == null) {
                return;
            }
            if (Unpooled.EMPTY_BUFFER.equals(content)) {
                return;
            }

            String contentTypeValue = headers.get(HttpHeaderNames.CONTENT_TYPE);
            boolean needCollectHttpBody = false;
            for (String contentType : NettyHttpPluginConfig.Plugin.NettyHttp.SUPPORTED_CONTENT_TYPES_PREFIX.split(",")) {
                if (contentTypeValue.startsWith(contentType)) {
                    needCollectHttpBody = true;
                    break;
                }
            }

            if (needCollectHttpBody) {
                String bodyStr = content.toString(getCharsetFromContentType(
                        headers.get(HttpHeaderNames.CONTENT_TYPE)));
                bodyStr = NettyHttpPluginConfig.Plugin.NettyHttp.FILTER_LENGTH_LIMIT > 0 ?
                        StringUtil.cut(bodyStr, NettyHttpPluginConfig.Plugin.NettyHttp.FILTER_LENGTH_LIMIT) : bodyStr;
                Tags.HTTP.BODY.set(span, bodyStr);
            }
        } catch (Exception e) {
            LOGGER.error("Fail to collect netty http request body", e);
        }

    }

    private static Charset getCharsetFromContentType(String contentType) {
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("charset=")) {
                return Charset.forName(part.substring("charset=".length()));
            }
        }
        return StandardCharsets.UTF_8;
    }
}
