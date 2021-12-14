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

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.plugin.customize.conf.CustomizeConfiguration;
import org.apache.skywalking.apm.plugin.customize.conf.MethodConfiguration;
import org.apache.skywalking.apm.plugin.customize.constants.Constants;
import org.apache.skywalking.apm.agent.core.util.CustomizeExpression;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BaseInterceptorMethods {

    void beforeMethod(Method method, Object[] allArguments) {
        Map<String, Object> configuration = CustomizeConfiguration.INSTANCE.getConfiguration(method);
        String operationName = MethodConfiguration.getOperationName(configuration);
        Map<String, Object> context = CustomizeExpression.evaluationContext(allArguments);
        if (context == null || context.isEmpty()) {
            ContextManager.createLocalSpan(operationName);
        } else {

            Map<String, String> tags = MethodConfiguration.getTags(configuration);
            Map<String, String> spanTags = new HashMap<String, String>();
            Map<String, String> logs = MethodConfiguration.getLogs(configuration);
            Map<String, String> spanLogs = new HashMap<String, String>();

            List<String> operationNameSuffixes = MethodConfiguration.getOperationNameSuffixes(configuration);
            StringBuilder operationNameSuffix = new StringBuilder();
            if (operationNameSuffixes != null && !operationNameSuffixes.isEmpty()) {
                for (String expression : operationNameSuffixes) {
                    operationNameSuffix.append(Constants.OPERATION_NAME_SEPARATOR);
                    operationNameSuffix.append(CustomizeExpression.parseExpression(expression, context));
                }
            }
            evalAndPopulate(context, tags, spanTags);
            evalAndPopulate(context, logs, spanLogs);

            operationName = operationNameSuffix.insert(0, operationName).toString();

            AbstractSpan span = ContextManager.createLocalSpan(operationName);
            tagSpanTags(span, spanTags);
            tagSpanLogs(span, spanLogs);
        }
    }

    void afterMethod(Method method, Object ret) {
        if (!ContextManager.isActive()) {
            return;
        }

        if (ret == null) {
            ContextManager.stopSpan();
            return;
        }

        Map<String, Object> configuration = CustomizeConfiguration.INSTANCE.getConfiguration(method);
        Map<String, Object> context = CustomizeExpression.evaluationReturnContext(ret);

        Map<String, String> tags = MethodConfiguration.getTags(configuration);
        Map<String, String> spanTags = new HashMap<String, String>();
        Map<String, String> logs = MethodConfiguration.getLogs(configuration);
        Map<String, String> spanLogs = new HashMap<String, String>();

        evalReturnAndPopulate(context, tags, spanTags);
        evalReturnAndPopulate(context, logs, spanLogs);

        final AbstractSpan localSpan = ContextManager.activeSpan();
        tagSpanTags(localSpan, spanTags);
        tagSpanLogs(localSpan, spanLogs);

        ContextManager.stopSpan(localSpan);

    }

    void handleMethodException(Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }

    private void evalAndPopulate(Map<String, Object> context, Map<String, String> exprMap, Map<String, String> toMap) {
        if (exprMap != null && !exprMap.isEmpty()) {
            for (Map.Entry<String, String> entry : exprMap.entrySet()) {
                String expression = entry.getValue();
                if (isReturnedObjExpression(expression)) {
                    continue;
                }
                toMap.put(entry.getKey(), CustomizeExpression.parseExpression(expression, context));
            }
        }
    }

    private void evalReturnAndPopulate(Map<String, Object> context, Map<String, String> exprMap, Map<String, String> toMap) {
        if (exprMap != null && !exprMap.isEmpty()) {
            for (Map.Entry<String, String> entry : exprMap.entrySet()) {
                String expression = entry.getValue();
                if (!isReturnedObjExpression(expression)) {
                    continue;
                }
                toMap.put(entry.getKey(), CustomizeExpression.parseReturnExpression(expression, context));
            }
        }
    }

    private void tagSpanTags(AbstractSpan span,  Map<String, String> spanTags) {
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
