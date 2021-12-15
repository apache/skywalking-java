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

package org.apache.skywalking.apm.plugin.customize.interceptor;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.agent.core.util.CustomizeExpression;
import org.apache.skywalking.apm.plugin.customize.conf.CustomizeConfiguration;
import org.apache.skywalking.apm.plugin.customize.conf.MethodConfiguration;
import org.apache.skywalking.apm.plugin.customize.constants.Constants;

class BaseInterceptorMethods {

    private static class SpanDataHolder {
        final AbstractSpan localSpan;

        final Map<String, String> tags;

        final Map<String, String> logs;

        final Map<String, String> spanTags;

        final Map<String, String> spanLogs;

        public SpanDataHolder(AbstractSpan localSpan, Map<String, String> tags,
            Map<String, String> logs, Map<String, String> spanTags, Map<String, String> spanLogs) {
            this.localSpan = localSpan;
            this.tags = tags;
            this.logs = logs;
            this.spanTags = spanTags;
            this.spanLogs = spanLogs;
        }
    }

    void beforeMethod(Method method, Object[] allArguments, MethodInvocationContext miContext) {
        Map<String, Object> configuration = CustomizeConfiguration.INSTANCE.getConfiguration(method);
        String operationName = MethodConfiguration.getOperationName(configuration);
        Map<String, Object> evalContext = CustomizeExpression.evaluationContext(allArguments);

        Map<String, String> tags = MethodConfiguration.getTags(configuration);
        Map<String, String> logs = MethodConfiguration.getLogs(configuration);
        Map<String, String> spanTags = tags == null ? Collections.EMPTY_MAP : new HashMap<String, String>(tags.size());
        Map<String, String> spanLogs = logs == null ? Collections.EMPTY_MAP : new HashMap<String, String>(logs.size());

        if (evalContext == null || evalContext.isEmpty()) {
            SpanDataHolder spanDataHolder = new SpanDataHolder(
                ContextManager.createLocalSpan(operationName),
                tags, logs, spanTags, spanLogs
            );
            miContext.setContext(spanDataHolder);
        } else {
            List<String> operationNameSuffixes = MethodConfiguration.getOperationNameSuffixes(configuration);
            StringBuilder operationNameSuffix = new StringBuilder();
            if (operationNameSuffixes != null && !operationNameSuffixes.isEmpty()) {
                for (String expression : operationNameSuffixes) {
                    operationNameSuffix.append(Constants.OPERATION_NAME_SEPARATOR);
                    operationNameSuffix.append(CustomizeExpression.parseExpression(expression, evalContext));
                }
            }
            evalAndPopulate(evalContext, false, tags, spanTags);
            evalAndPopulate(evalContext, false, logs, spanLogs);

            operationName = operationNameSuffix.insert(0, operationName).toString();
            AbstractSpan localSpan = ContextManager.createLocalSpan(operationName);

            tagSpanTags(localSpan, spanTags);
            tagSpanLogs(localSpan, spanLogs);

            spanTags.clear();
            spanLogs.clear();

            SpanDataHolder spanDataHolder = new SpanDataHolder(
                localSpan, tags, logs, spanTags, spanLogs
            );
            miContext.setContext(spanDataHolder);
        }
    }

    void afterMethod(Method method, Object ret, MethodInvocationContext miContext) {
        if (!ContextManager.isActive()) {
            return;
        }
        SpanDataHolder spanDataHolder = (SpanDataHolder) miContext.getContext();
        if (spanDataHolder == null || spanDataHolder.localSpan == null) {
            return;
        }
        AbstractSpan localSpan = spanDataHolder.localSpan;
        if (ret == null) {
            ContextManager.stopSpan(localSpan);
            return;
        }
        Map<String, String> tags = spanDataHolder.tags;
        Map<String, String> logs = spanDataHolder.logs;
        Map<String, String> spanTags = spanDataHolder.spanTags;
        Map<String, String> spanLogs = spanDataHolder.spanLogs;

        try {
            Map<String, Object> evalContext = CustomizeExpression.evaluationReturnContext(ret);

            evalAndPopulate(evalContext, true, tags, spanTags);
            evalAndPopulate(evalContext, true, logs, spanLogs);

            tagSpanTags(localSpan, spanTags);
            tagSpanLogs(localSpan, spanLogs);

            spanTags.clear();
            spanLogs.clear();
        } finally {
            ContextManager.stopSpan(localSpan);
        }
    }

    void handleMethodException(Throwable t) {
        ContextManager.activeSpan().log(t);
    }

    private void evalAndPopulate(Map<String, Object> context, boolean returnExpr, Map<String, String> exprMap,
        Map<String, String> toMap) {
        if (exprMap != null && !exprMap.isEmpty()) {
            for (Map.Entry<String, String> entry : exprMap.entrySet()) {
                String expression = entry.getValue();
                if (isReturnedObjExpression(expression) != returnExpr) {
                    continue;
                }
                toMap.put(entry.getKey(), CustomizeExpression.parseExpression(expression, context));
            }
        }
    }

    private void tagSpanTags(AbstractSpan span, Map<String, String> spanTags) {
        if (spanTags != null && !spanTags.isEmpty()) {
            for (Map.Entry<String, String> tag : spanTags.entrySet()) {
                span.tag(Tags.ofKey(tag.getKey()), tag.getValue());
            }
        }
    }

    private void tagSpanLogs(AbstractSpan span, Map<String, String> spanLogs) {
        if (spanLogs != null && !spanLogs.isEmpty()) {
            span.log(System.currentTimeMillis(), spanLogs);
        }
    }

    private boolean isReturnedObjExpression(String expression) {
        String[] es = expression.split("\\.");
        return "returnedObj".equals(es[0]);
    }
}
