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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class HttpClientDoExecuteInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String ERROR_URI = "/_blank";

    private static final ILog LOGGER = LogManager.getLogger(HttpClientDoExecuteInterceptor.class);

    /**
     * Lazily-resolved, immutable set of ports that must not receive SkyWalking
     * propagation headers.  Built once from
     * {@link HttpClient5PluginConfig.Plugin.HttpClient5#PROPAGATION_EXCLUDE_PORTS}.
     */
    private volatile Set<Integer> excludePortsCache;

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            MethodInterceptResult result) throws Throwable {
        if (skipIntercept(objInst, method, allArguments, argumentsTypes)) {
            // illegal args, can't trace. ignore.
            return;
        }
        final HttpHost httpHost = getHttpHost(objInst, method, allArguments, argumentsTypes);
        ClassicHttpRequest httpRequest = (ClassicHttpRequest) allArguments[1];
        final ContextCarrier contextCarrier = new ContextCarrier();

        String remotePeer = httpHost.getHostName() + ":" + port(httpHost);

        String uri = httpRequest.getUri().toString();
        String requestURI = getRequestURI(uri);
        String operationName = requestURI;
        AbstractSpan span = ContextManager.createExitSpan(operationName, contextCarrier, remotePeer);
        if (ERROR_URI.equals(requestURI)) {
            span.errorOccurred();
        }
        span.setComponent(ComponentsDefine.HTTPCLIENT);
        Tags.URL.set(span, buildURL(httpHost, uri));
        Tags.HTTP.METHOD.set(span, httpRequest.getMethod());
        SpanLayer.asHttp(span);

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            httpRequest.setHeader(next.getHeadKey(), next.getHeadValue());
        }
    }

    protected boolean skipIntercept(EnhancedInstance objInst, Method method, Object[] allArguments,
                                    Class<?>[] argumentsTypes) {
        if (allArguments[1] == null) {
            return true;
        }
        HttpHost host = getHttpHost(objInst, method, allArguments, argumentsTypes);
        if (host == null) {
            return true;
        }
        return isExcludedPort(host.getPort());
    }

    /**
     * Returns {@code true} when {@code port} is listed in
     * {@link HttpClient5PluginConfig.Plugin.HttpClient5#PROPAGATION_EXCLUDE_PORTS}.
     *
     * <p>The config value is parsed lazily and cached so that it is read after
     * the agent has fully initialised its configuration subsystem.
     */
    private boolean isExcludedPort(int port) {
        if (port <= 0) {
            return false;
        }
        if (excludePortsCache == null) {
            synchronized (this) {
                if (excludePortsCache == null) {
                    excludePortsCache = parseExcludePorts(
                            HttpClient5PluginConfig.Plugin.HttpClient5.PROPAGATION_EXCLUDE_PORTS);
                }
            }
        }
        return excludePortsCache.contains(port);
    }

    private static Set<Integer> parseExcludePorts(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .map(s -> {
                         try {
                             return Integer.parseInt(s);
                         } catch (NumberFormatException e) {
                             LOGGER.warn("Ignoring invalid port in PROPAGATION_EXCLUDE_PORTS: {}", s);
                             return -1;
                         }
                     })
                     .filter(p -> p > 0)
                     .collect(Collectors.toSet());
    }

    protected abstract HttpHost getHttpHost(EnhancedInstance objInst, Method method, Object[] allArguments,
                                   Class<?>[] argumentsTypes) ;

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            Object ret) throws Throwable {
        if (skipIntercept(objInst, method, allArguments, argumentsTypes)) {
            return ret;
        }

        if (ret != null) {
            ClassicHttpResponse response = (ClassicHttpResponse) ret;

            int statusCode = response.getCode();
            AbstractSpan span = ContextManager.activeSpan();
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
            if (statusCode >= 400) {
                span.errorOccurred();
            }
        }

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Throwable t) {
        if (skipIntercept(objInst, method, allArguments, argumentsTypes)) {
            return;
        }
        AbstractSpan activeSpan = ContextManager.activeSpan();
        activeSpan.log(t);
    }

    private String getRequestURI(String uri) {
        if (isUrl(uri)) {
            String requestPath;
            try {
                requestPath = new URL(uri).getPath();
            } catch (MalformedURLException e) {
                return ERROR_URI;
            }
            return requestPath != null && requestPath.length() > 0 ? requestPath : "/";
        } else {
            return uri;
        }
    }

    private boolean isUrl(String uri) {
        String lowerUrl = uri.toLowerCase();
        return lowerUrl.startsWith("http") || lowerUrl.startsWith("https");
    }

    private String buildURL(HttpHost httpHost, String uri) {
        if (isUrl(uri)) {
            return uri;
        } else {
            StringBuilder buff = new StringBuilder();
            buff.append(httpHost.getSchemeName().toLowerCase());
            buff.append("://");
            buff.append(httpHost.getHostName());
            buff.append(":");
            buff.append(port(httpHost));
            buff.append(uri);
            return buff.toString();
        }
    }

    private int port(HttpHost httpHost) {
        int port = httpHost.getPort();
        return port > 0 ? port : "https".equals(httpHost.getSchemeName().toLowerCase()) ? 443 : 80;
    }
}
