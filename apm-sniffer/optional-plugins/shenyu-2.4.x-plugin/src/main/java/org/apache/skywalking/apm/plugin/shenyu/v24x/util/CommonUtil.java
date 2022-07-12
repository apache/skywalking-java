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

package org.apache.skywalking.apm.plugin.shenyu.v24x.util;

import static org.apache.skywalking.apm.plugin.shenyu.v24x.Constant.PROXY_RPC_SPAN;

import java.util.Objects;
import java.util.Optional;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

public class CommonUtil {

    public static EnhancedInstance getEnhancedServerWebExchange(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof DefaultServerWebExchange) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getEnhancedServerWebExchange(delegate);
        }
        return instance;
    }

    public static void createLocalSpan(Object[] allArguments, String localOp) {
        ServerWebExchange exchange = (ServerWebExchange) allArguments[0];
        EnhancedInstance enhancedInstance = CommonUtil.getEnhancedServerWebExchange(allArguments[0]);
        AbstractSpan span = ContextManager.createLocalSpan(localOp);
        span.setComponent(ComponentsDefine.APACHE_SHENYU);
        SpanLayer.asRPCFramework(span);
        if (enhancedInstance != null && enhancedInstance.getSkyWalkingDynamicField() != null) {
            ContextManager.continued((ContextSnapshot) enhancedInstance.getSkyWalkingDynamicField());
        }
        exchange.getAttributes().put(PROXY_RPC_SPAN, span);
    }

    public static Object stopLocalSpan(Object[] allArguments, Object ret) {
        ServerWebExchange exchange = (ServerWebExchange) allArguments[0];
        AbstractSpan span = (AbstractSpan) exchange.getAttributes().get(PROXY_RPC_SPAN);
        if (Objects.isNull(span)) {
            return ret;
        }
        Optional.ofNullable(exchange.getResponse().getStatusCode()).ifPresent(httpStatus -> {
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, httpStatus.value());
            if (httpStatus.isError()) {
                span.errorOccurred();
            }
        });
        ContextManager.stopSpan(span);
        return ret;
    }

    public static void logErrorLocalSpan(Object[] allArguments, Throwable t) {
        ServerWebExchange exchange = (ServerWebExchange) allArguments[0];
        AbstractSpan span = (AbstractSpan) exchange.getAttributes().get(PROXY_RPC_SPAN);
        if (Objects.isNull(span)) {
            return ;
        }
        span.errorOccurred().log(t);
    }
}
