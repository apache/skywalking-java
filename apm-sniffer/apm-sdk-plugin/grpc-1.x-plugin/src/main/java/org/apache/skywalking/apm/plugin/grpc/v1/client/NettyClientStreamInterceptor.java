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

package org.apache.skywalking.apm.plugin.grpc.v1.client;

import static org.apache.skywalking.apm.plugin.grpc.v1.Constants.CLIENT_STREAM_PEER;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * Intercept constructor to obtain server IP.
 */
@Slf4j
public class NettyClientStreamInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) throws Throwable {
        String authorityClass = allArguments[4].getClass().getName();
        if ("io.netty.util.AsciiString".equals(authorityClass)
                || "io.grpc.netty.shaded.io.netty.util.AsciiString".equals(authorityClass)) {
            ContextManager.getRuntimeContext().put(CLIENT_STREAM_PEER, allArguments[4].toString());
        }
    }

}
