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

package org.apache.skywalking.apm.plugin.solon;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;
import org.noear.solon.Utils;
import org.noear.solon.core.handle.Context;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SolonActionExecuteInterceptor implements InstanceMethodsAroundInterceptor {

    private static FastPathMatcher pathMatcher = new FastPathMatcher();

    private final Set<String> excludePaths = new HashSet<>();

    private final Set<String> matchPaths = new HashSet<>();

    public SolonActionExecuteInterceptor() {
        String excluded = System.getProperty("skywalking.agent.solon.excluded", "");
        if (Utils.isNotEmpty(excluded)) {
            for (String path : excluded.split(",")) {
                path = path.trim();
                if (!path.isEmpty()) {
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    if (path.contains("*")) {
                        matchPaths.add(path);
                    } else {
                        excludePaths.add(path);
                    }
                }
            }
        }
    }

    public boolean pathMatch(String path) {
        for (String matchPath : matchPaths) {
            if (pathMatcher.match(matchPath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        Context ctx = (Context) allArguments[0];
        if (excludePaths.contains(ctx.pathNew()) || pathMatch(ctx.pathNew())) {
            return;
        }
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(ctx.header(next.getHeadKey()));
        }
        String operationName = "Solon:" + ctx.method() + ":" + ctx.path();
        AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
        span.setComponent(ComponentsDefine.SOLON_MVC);
        SpanLayer.asHttp(span);
        span.tag("http.method", ctx.method());
        span.tag("http.path", ctx.path());
        span.tag("framework", "solon");
        for (Map.Entry<String, String> stringStringEntry : ctx.headerMap().entrySet()) {
            span.tag(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        String body = ctx.body();
        if (StringUtil.isNotBlank(body)) {
            if (body.length() > 1024) {
                body = body.substring(0, 1024);
            }
            span.tag("http.body", body);
        }
        String param = ctx.paramMap().toString();
        if (param.length() > 1024) {
            param = param.substring(0, 1024);
        }
        span.tag("http.param", param);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) {
        Context ctx = (Context) allArguments[0];
        if (excludePaths.contains(ctx.pathNew()) || pathMatch(ctx.pathNew())) {
            return ret;
        }

        ContextManager.activeSpan().tag("http.status_code", String.valueOf(ctx.status()));
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan activeSpan = ContextManager.activeSpan();
        activeSpan.log(t);
    }
}
