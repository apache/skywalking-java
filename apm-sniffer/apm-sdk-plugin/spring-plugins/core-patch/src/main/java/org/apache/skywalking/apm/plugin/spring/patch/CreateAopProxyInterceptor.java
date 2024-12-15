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

package org.apache.skywalking.apm.plugin.spring.patch;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.AdvisedSupport;

import java.lang.reflect.Method;

/**
 * <code>CreateAopProxyInterceptor</code> check that the bean has been implement {@link EnhancedInstance}.
 * if yes, true will be returned.
 */
public class CreateAopProxyInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        AdvisedSupport advisedSupport = (AdvisedSupport) allArguments[0];

        if (maybeHasUserSuppliedProxyInterfaces(ret)) {
            Class targetClass = advisedSupport.getTargetClass();
            if (targetClass != null) {
                if (onlyImplementsEnhancedInstance(advisedSupport) || onlyImplementsEnhancedInstanceAndSpringProxy(advisedSupport)) {
                    return true;
                }
            }
        }
        return ret;
    }

    private boolean maybeHasUserSuppliedProxyInterfaces(Object ret) {
        return !(Boolean) ret;
    }

    private boolean onlyImplementsEnhancedInstanceAndSpringProxy(AdvisedSupport advisedSupport) {
        Class<?>[] ifcs = advisedSupport.getProxiedInterfaces();
        Class targetClass = advisedSupport.getTargetClass();
        return ifcs.length == 2 && EnhancedInstance.class.isAssignableFrom(targetClass) && SpringProxy.class.isAssignableFrom(targetClass);
    }

    private boolean onlyImplementsEnhancedInstance(AdvisedSupport advisedSupport) {
        Class<?>[] ifcs = advisedSupport.getProxiedInterfaces();
        Class targetClass = advisedSupport.getTargetClass();
        return ifcs.length == 1 && EnhancedInstance.class.isAssignableFrom(targetClass);
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {

    }
}
