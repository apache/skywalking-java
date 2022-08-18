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

package org.apache.skywalking.apm.plugin.xxljob23;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

import static org.apache.skywalking.apm.plugin.xxljob23.Constants.JOB_PARAM;
import static org.apache.skywalking.apm.plugin.xxljob23.Constants.XXL_JOB_HELPER;
import static org.apache.skywalking.apm.plugin.xxljob23.Constants.XXL_JOB_HELPER_GET_PARAM_METHOD;

/**
 * Intercept method of {@link com.xxl.job.core.handler.impl.MethodJobHandler#execute()}.
 * record the xxl-job method job local span.
 */
public class MethodJobHandlerMethodInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        String methodName = (String) objInst.getSkyWalkingDynamicField();
        String operationName = ComponentsDefine.XXL_JOB.getName() + "/MethodJob/" + methodName;

        AbstractSpan span = ContextManager.createLocalSpan(operationName);
        span.setComponent(ComponentsDefine.XXL_JOB);
        Tags.LOGIC_ENDPOINT.set(span, Tags.VAL_LOCAL_SPAN_AS_LOGIC_ENDPOINT);

        Class<?> xxlJobHelper = Class.forName(XXL_JOB_HELPER);
        Method getJobParamMethod = xxlJobHelper.getMethod(XXL_JOB_HELPER_GET_PARAM_METHOD);
        String jobParam = (String) getJobParamMethod.invoke(null);

        span.tag(JOB_PARAM, jobParam);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
