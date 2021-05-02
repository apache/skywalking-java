/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.plugin.spring.cloud.gateway.v3x.define.EnhanceObjectCache;
import reactor.netty.http.client.HttpClientConfig;

/**
 * Intercept the constructor and inject {@link EnhanceObjectCache}.
 * <p>
 * The first constructor argument is {@link reactor.netty.http.client.HttpClientConfig} class instance which can get the
 * request uri string.
 */
public class HttpClientFinalizerConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        final HttpClientConfig httpClientConfig = (HttpClientConfig) allArguments[0];
        if (httpClientConfig == null) {
            return;
        }
        final EnhanceObjectCache enhanceObjectCache = new EnhanceObjectCache();
        enhanceObjectCache.setUrl(httpClientConfig.uri());
        objInst.setSkyWalkingDynamicField(enhanceObjectCache);
    }
}
