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

package org.apache.skywalking.apm.plugin.spring.mvc.commons.interceptor;

import com.google.common.collect.Maps;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.RuntimeContext;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.util.CollectionUtil;
import org.apache.skywalking.apm.agent.core.util.MethodUtil;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.spring.mvc.commons.EnhanceRequireObjectCache;
import org.apache.skywalking.apm.plugin.spring.mvc.commons.RequestUtil;
import org.apache.skywalking.apm.plugin.spring.mvc.commons.SpringMVCPluginConfig;
import org.apache.skywalking.apm.plugin.spring.mvc.commons.exception.IllegalMethodStackDepthException;
import org.apache.skywalking.apm.plugin.spring.mvc.commons.exception.ServletResponseNotFoundException;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.skywalking.apm.agent.core.util.ClassUtil.getAllSuperClassesAndInterfaces;
import static org.apache.skywalking.apm.plugin.spring.mvc.commons.Constants.CONTROLLER_METHOD_STACK_DEPTH;
import static org.apache.skywalking.apm.plugin.spring.mvc.commons.Constants.FORWARD_REQUEST_FLAG;
import static org.apache.skywalking.apm.plugin.spring.mvc.commons.Constants.REACTIVE_ASYNC_SPAN_IN_RUNTIME_CONTEXT;
import static org.apache.skywalking.apm.plugin.spring.mvc.commons.Constants.REQUEST_KEY_IN_RUNTIME_CONTEXT;
import static org.apache.skywalking.apm.plugin.spring.mvc.commons.Constants.RESPONSE_KEY_IN_RUNTIME_CONTEXT;

/**
 * the abstract method interceptor
 */
public abstract class AbstractMethodInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String SERVLET_RESPONSE_CLASS = "javax.servlet.http.HttpServletResponse";
    private static final String JAKARTA_SERVLET_RESPONSE_CLASS = "jakarta.servlet.http.HttpServletResponse";
    private static final String SPRING_REACTIVE_RESPONSE_CLASS = "org.springframework.http.server.reactive.ServerHttpResponse";

    private static final String JAVAX_SERVLET_REQUEST_CLASS = "javax.servlet.http.HttpServletRequest";
    private static final String JAKARTA_SERVLET_REQUEST_CLASS = "jakarta.servlet.http.HttpServletRequest";
    private static final String SPRING_REACTIVE_REQUEST_CLASS = "org.springframework.http.server.reactive.ServerHttpRequest";

    private static final Map<Map.Entry<String, Class<?>>, Boolean> CACHE = new ConcurrentHashMap<>(10);

    public abstract String getRequestURL(Method method);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

        Boolean forwardRequestFlag = (Boolean) ContextManager.getRuntimeContext().get(FORWARD_REQUEST_FLAG);
        /**
         * Spring MVC plugin do nothing if current request is forward request.
         * Ref: https://github.com/apache/skywalking/pull/1325
         */
        if (forwardRequestFlag != null && forwardRequestFlag) {
            return;
        }

        Object request = ContextManager.getRuntimeContext().get(REQUEST_KEY_IN_RUNTIME_CONTEXT);

        if (request != null) {
            StackDepth stackDepth = (StackDepth) ContextManager.getRuntimeContext().get(CONTROLLER_METHOD_STACK_DEPTH);

            if (stackDepth == null) {
                final ContextCarrier contextCarrier = new ContextCarrier();

                if (isAssignableFrom(JAVAX_SERVLET_REQUEST_CLASS, request.getClass())) {
                    final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                    CarrierItem next = contextCarrier.items();
                    while (next.hasNext()) {
                        next = next.next();
                        next.setHeadValue(httpServletRequest.getHeader(next.getHeadKey()));
                    }

                    String operationName = this.buildOperationName(method, httpServletRequest.getMethod(),
                            (EnhanceRequireObjectCache) objInst.getSkyWalkingDynamicField());
                    AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
                    Tags.URL.set(span, httpServletRequest.getRequestURL().toString());
                    Tags.HTTP.METHOD.set(span, httpServletRequest.getMethod());
                    span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
                    SpanLayer.asHttp(span);

                    if (SpringMVCPluginConfig.Plugin.SpringMVC.COLLECT_HTTP_PARAMS) {
                        RequestUtil.collectHttpParam(httpServletRequest, span);
                    }

                    if (!CollectionUtil.isEmpty(SpringMVCPluginConfig.Plugin.Http.INCLUDE_HTTP_HEADERS)) {
                        RequestUtil.collectHttpHeaders(httpServletRequest, span);
                    }
                } else if (isAssignableFrom(JAKARTA_SERVLET_REQUEST_CLASS, request.getClass())) {
                    final jakarta.servlet.http.HttpServletRequest httpServletRequest = (jakarta.servlet.http.HttpServletRequest) request;
                    CarrierItem next = contextCarrier.items();
                    while (next.hasNext()) {
                        next = next.next();
                        next.setHeadValue(httpServletRequest.getHeader(next.getHeadKey()));
                    }

                    String operationName =
                            this.buildOperationName(method, httpServletRequest.getMethod(),
                                    (EnhanceRequireObjectCache) objInst.getSkyWalkingDynamicField());
                    AbstractSpan span =
                            ContextManager.createEntrySpan(operationName, contextCarrier);
                    Tags.URL.set(span, httpServletRequest.getRequestURL().toString());
                    Tags.HTTP.METHOD.set(span, httpServletRequest.getMethod());
                    span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
                    SpanLayer.asHttp(span);

                    if (SpringMVCPluginConfig.Plugin.SpringMVC.COLLECT_HTTP_PARAMS) {
                        RequestUtil.collectHttpParam(httpServletRequest, span);
                    }

                    if (!CollectionUtil
                            .isEmpty(SpringMVCPluginConfig.Plugin.Http.INCLUDE_HTTP_HEADERS)) {
                        RequestUtil.collectHttpHeaders(httpServletRequest, span);
                    }
                } else if (isAssignableFrom(SPRING_REACTIVE_REQUEST_CLASS, request.getClass())) {
                    final ServerHttpRequest serverHttpRequest = (ServerHttpRequest) request;
                    CarrierItem next = contextCarrier.items();
                    while (next.hasNext()) {
                        next = next.next();
                        next.setHeadValue(serverHttpRequest.getHeaders().getFirst(next.getHeadKey()));
                    }

                    String operationName = this.buildOperationName(method, serverHttpRequest.getMethod().name(),
                            (EnhanceRequireObjectCache) objInst.getSkyWalkingDynamicField());
                    AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
                    Tags.URL.set(span, serverHttpRequest.getURI().toString());
                    Tags.HTTP.METHOD.set(span, serverHttpRequest.getMethod().name());
                    span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
                    SpanLayer.asHttp(span);

                    if (SpringMVCPluginConfig.Plugin.SpringMVC.COLLECT_HTTP_PARAMS) {
                        RequestUtil.collectHttpParam(serverHttpRequest, span);
                    }

                    if (!CollectionUtil.isEmpty(SpringMVCPluginConfig.Plugin.Http.INCLUDE_HTTP_HEADERS)) {
                        RequestUtil.collectHttpHeaders(serverHttpRequest, span);
                    }
                } else {
                    throw new IllegalStateException("this line should not be reached");
                }

                stackDepth = new StackDepth();
                ContextManager.getRuntimeContext().put(CONTROLLER_METHOD_STACK_DEPTH, stackDepth);
            } else {
                AbstractSpan span = ContextManager.createLocalSpan(buildOperationName(objInst, method));
                span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
            }

            stackDepth.increment();
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        final RuntimeContext runtimeContext = ContextManager.getRuntimeContext();
        Boolean forwardRequestFlag = (Boolean) runtimeContext.get(FORWARD_REQUEST_FLAG);
        /**
         * Spring MVC plugin do nothing if current request is forward request.
         * Ref: https://github.com/apache/skywalking/pull/1325
         */
        if (forwardRequestFlag != null && forwardRequestFlag) {
            return ret;
        }

        Object request = runtimeContext.get(REQUEST_KEY_IN_RUNTIME_CONTEXT);

        if (request != null) {
            try {
                StackDepth stackDepth = (StackDepth) runtimeContext.get(CONTROLLER_METHOD_STACK_DEPTH);
                if (stackDepth == null) {
                    throw new IllegalMethodStackDepthException();
                } else {
                    stackDepth.decrement();
                }

                AbstractSpan span = ContextManager.activeSpan();

                if (stackDepth.depth() == 0) {
                    Object response = runtimeContext.get(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
                    if (response == null) {
                        throw new ServletResponseNotFoundException();
                    }

                    Integer statusCode = null;

                    if (isAssignableFrom(SERVLET_RESPONSE_CLASS, response.getClass())) {
                        statusCode = ((HttpServletResponse) response).getStatus();
                    } else if (isAssignableFrom(JAKARTA_SERVLET_RESPONSE_CLASS, response.getClass())) {
                        statusCode = ((jakarta.servlet.http.HttpServletResponse) response).getStatus();
                    } else if (isAssignableFrom(SPRING_REACTIVE_RESPONSE_CLASS, response.getClass())) {
                        statusCode = ((ServerHttpResponse) response).getRawStatusCode();
                        Object context = runtimeContext.get(REACTIVE_ASYNC_SPAN_IN_RUNTIME_CONTEXT);
                        if (context != null) {
                            ((AbstractSpan[]) context)[0] = span.prepareForAsync();
                        }
                    }

                    if (statusCode != null) {
                        Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
                        if (statusCode >= 400) {
                            span.errorOccurred();
                        }
                    }

                    runtimeContext.remove(REACTIVE_ASYNC_SPAN_IN_RUNTIME_CONTEXT);
                    runtimeContext.remove(REQUEST_KEY_IN_RUNTIME_CONTEXT);
                    runtimeContext.remove(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
                    runtimeContext.remove(CONTROLLER_METHOD_STACK_DEPTH);
                }

                // Active HTTP parameter collection automatically in the profiling context.
                if (!SpringMVCPluginConfig.Plugin.SpringMVC.COLLECT_HTTP_PARAMS && span.isProfiling()) {
                    if (isAssignableFrom(JAVAX_SERVLET_REQUEST_CLASS, request.getClass())) {
                        RequestUtil.collectHttpParam((HttpServletRequest) request, span);
                    } else if (isAssignableFrom(JAKARTA_SERVLET_REQUEST_CLASS, request.getClass())) {
                        RequestUtil.collectHttpParam((jakarta.servlet.http.HttpServletRequest) request, span);
                    } else if (isAssignableFrom(SPRING_REACTIVE_REQUEST_CLASS, request.getClass())) {
                        RequestUtil.collectHttpParam((ServerHttpRequest) request, span);
                    }
                }
            } finally {
                ContextManager.stopSpan();
            }
        }

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private String buildOperationName(Object invoker, Method method) {
        StringBuilder operationName = new StringBuilder(invoker.getClass().getName()).append(".")
                .append(method.getName())
                .append("(");
        for (Class<?> type : method.getParameterTypes()) {
            operationName.append(type.getName()).append(",");
        }

        if (method.getParameterTypes().length > 0) {
            operationName = operationName.deleteCharAt(operationName.length() - 1);
        }

        return operationName.append(")").toString();
    }

    private String buildOperationName(Method method, String httpMethod, EnhanceRequireObjectCache pathMappingCache) {
        String operationName;
        if (SpringMVCPluginConfig.Plugin.SpringMVC.USE_QUALIFIED_NAME_AS_ENDPOINT_NAME) {
            operationName = MethodUtil.generateOperationName(method);
        } else {
            String requestURL = pathMappingCache.findPathMapping(method);
            if (requestURL == null) {
                requestURL = getRequestURL(method);
                pathMappingCache.addPathMapping(method, requestURL);
                requestURL = pathMappingCache.findPathMapping(method);
            }
            operationName = String.join(":", httpMethod, requestURL);
        }

        return operationName;
    }

    private boolean isAssignableFrom(String superClassName, Class<?> subClass) {
        if (superClassName == null || subClass == null) {
            return false;
        }

        Map.Entry<String, Class<?>> key = Maps.immutableEntry(superClassName, subClass);
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        Set<String> allSuperClassesAndInterfaces = getAllSuperClassesAndInterfaces(subClass);
        if (allSuperClassesAndInterfaces.contains(superClassName)) {
            CACHE.put(key, true);
            return true;
        }
        return false;
    }
}
