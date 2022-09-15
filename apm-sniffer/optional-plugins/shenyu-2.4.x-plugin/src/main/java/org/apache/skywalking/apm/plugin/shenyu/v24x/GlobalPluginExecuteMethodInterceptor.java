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

package org.apache.skywalking.apm.plugin.shenyu.v24x;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.ids.DistributedTraceId;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.tag.Tags.HTTP;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.apache.skywalking.apm.plugin.shenyu.v24x.Constant.SKYWALKING_CONTEXT_SNAPSHOT;

/**
 * Apache shenyu global-plugin interceptor.
 */
public class GlobalPluginExecuteMethodInterceptor implements InstanceMethodsAroundInterceptor {

    public static final String SHENYU_AGENT_TRACE_ID = "shenyu-agent-trace-id";

    public static final String SKYWALKING_SPAN = "SKYWALKING_SPAN";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        ServerWebExchange exchange = (ServerWebExchange) allArguments[0];

        ContextCarrier carrier = new ContextCarrier();
        CarrierItem next = carrier.items();
        HttpHeaders headers = exchange.getRequest().getHeaders();
        while (next.hasNext()) {
            next = next.next();
            List<String> header = headers.get(next.getHeadKey());
            if (header != null && header.size() > 0) {
                next.setHeadValue(header.get(0));
            }
        }

        AbstractSpan span = ContextManager.createEntrySpan(exchange.getRequest().getURI().getPath(), carrier);
        span.setComponent(ComponentsDefine.APACHE_SHENYU);
        SpanLayer.asHttp(span);
        Tags.URL.set(span, exchange.getRequest().getURI().toString());
        HTTP.METHOD.set(span, exchange.getRequest().getMethodValue());

        ContextSnapshot snapshot = ContextManager.capture();
        exchange.getAttributes().put(SHENYU_AGENT_TRACE_ID,
                Optional.ofNullable(snapshot.getTraceId()).map(DistributedTraceId::getId).orElse(ContextManager.getGlobalTraceId()));
        EnhancedInstance instance = getInstance(allArguments[0]);
        instance.setSkyWalkingDynamicField(snapshot);
        span.prepareForAsync();
        ContextManager.stopSpan(span);

        exchange.getAttributes().put(SKYWALKING_SPAN, span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {

        ServerWebExchange exchange = (ServerWebExchange) allArguments[0];

        AbstractSpan span = (AbstractSpan) exchange.getAttributes().get(SKYWALKING_SPAN);
        if (Objects.isNull(span)) {
            return ret;
        }
        Mono<Void> monoReturn = (Mono<Void>) ret;

        // add skywalking context snapshot to reactor context.
        // webclient plugin need to use SKYWALKING_CONTEXT_SNAPSHOT
        EnhancedInstance instance = getInstance(allArguments[0]);
        if (instance != null && instance.getSkyWalkingDynamicField() != null) {
            monoReturn = monoReturn.subscriberContext(
                    c -> c.put(SKYWALKING_CONTEXT_SNAPSHOT, instance.getSkyWalkingDynamicField()));
        }

        return monoReturn
                .doOnError(throwable -> span.errorOccurred().log(throwable))
                .doFinally(s -> {
                    try {
                        Optional.ofNullable(exchange.getResponse().getStatusCode()).ifPresent(httpStatus -> {
                            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, httpStatus.value());
                            if (httpStatus.isError()) {
                                span.errorOccurred();
                            }
                        });
                    } finally {
                        span.asyncFinish();
                    }
                });
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }

    public static EnhancedInstance getInstance(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof DefaultServerWebExchange) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getInstance(delegate);
        }
        return instance;
    }
}
