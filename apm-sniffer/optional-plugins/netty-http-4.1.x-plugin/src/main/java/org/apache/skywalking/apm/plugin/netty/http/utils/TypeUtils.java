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

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

public class TypeUtils {

    public static boolean isHttpResponse(Object obj) {
        Class<?> objClass = obj.getClass();
        if (objClass == DefaultFullHttpResponse.class || objClass == DefaultHttpResponse.class) {
            return true;
        }

        return obj instanceof HttpResponse;
    }

    public static boolean isHttpRequest(Object msg) {
        Class<?> objClass = msg.getClass();
        if (objClass == DefaultFullHttpRequest.class || objClass == DefaultHttpRequest.class) {
            return true;
        }

        return msg instanceof HttpRequest;
    }

    public static boolean isLastHttpContent(Object msg) {
        Class<?> objClass = msg.getClass();
        if (objClass == DefaultLastHttpContent.class) {
            return true;
        }

        return msg instanceof LastHttpContent;
    }

    public static boolean isFullHttpRequest(Object obj) {
        Class<?> objClass = obj.getClass();
        if (objClass == DefaultFullHttpRequest.class) {
            return true;
        }

        return obj instanceof FullHttpRequest;
    }
}
