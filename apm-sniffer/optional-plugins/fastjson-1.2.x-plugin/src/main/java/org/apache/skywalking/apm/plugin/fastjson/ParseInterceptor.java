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

package org.apache.skywalking.apm.plugin.fastjson;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.fastjson.common.Constants;

import java.lang.reflect.Method;

public class ParseInterceptor implements StaticMethodsAroundInterceptor {

    public static final String OPERATION_NAME_FROM_JSON = "Fastjson/";

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, MethodInterceptResult result) {

        AbstractSpan span = ContextManager.createLocalSpan(OPERATION_NAME_FROM_JSON + method.getName());
        span.setComponent(ComponentsDefine.FASTJSON);

        if (allArguments[0] instanceof String) {
            span.tag(Constants.SPAN_TAG_KEY_LENGTH, Integer.toString(((String) allArguments[0]).length()));
        } else if (allArguments[0] instanceof byte[]) {
            span.tag(Constants.SPAN_TAG_KEY_LENGTH, Integer.toString(((byte[]) allArguments[0]).length));
        }
    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Object ret) {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}