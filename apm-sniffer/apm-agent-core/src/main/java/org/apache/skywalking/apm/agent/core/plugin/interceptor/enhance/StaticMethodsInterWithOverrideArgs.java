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

package org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance;

import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.skywalking.apm.agent.core.plugin.loader.InterceptorInstanceLoader;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.so11y.AgentSO11Y;

/**
 * The actual byte-buddy's interceptor to intercept class static methods. In this class, it provides a bridge between
 * byte-buddy and sky-walking plugin.
 */
public class StaticMethodsInterWithOverrideArgs {
    private static final ILog LOGGER = LogManager.getLogger(StaticMethodsInterWithOverrideArgs.class);

    private String pluginName;
    /**
     * A class full name, and instanceof {@link StaticMethodsAroundInterceptor} This name should only stay in {@link
     * String}, the real {@link Class} type will trigger classloader failure. If you want to know more, please check on
     * books about Classloader or Classloader appointment mechanism.
     */
    private String staticMethodsAroundInterceptorClassName;

    private static final String INTERCEPTOR_TYPE = "static";

    /**
     * Set the name of {@link StaticMethodsInterWithOverrideArgs#staticMethodsAroundInterceptorClassName}
     *
     * @param pluginName name of interceptor plugin
     * @param staticMethodsAroundInterceptorClassName class full name.
     */
    public StaticMethodsInterWithOverrideArgs(String pluginName, String staticMethodsAroundInterceptorClassName) {
        this.pluginName = pluginName;
        this.staticMethodsAroundInterceptorClassName = staticMethodsAroundInterceptorClassName;
    }

    /**
     * Intercept the target static method.
     *
     * @param clazz        target class
     * @param allArguments all method arguments
     * @param method       method description.
     * @param zuper        the origin call ref.
     * @return the return value of target static method.
     * @throws Exception only throw exception because of zuper.call() or unexpected exception in sky-walking ( This is a
     *                   bug, if anything triggers this condition ).
     */
    @RuntimeType
    public Object intercept(@Origin Class<?> clazz, @AllArguments Object[] allArguments, @Origin Method method,
        @Morph OverrideCallable zuper) throws Throwable {
        StaticMethodsAroundInterceptor interceptor = InterceptorInstanceLoader.load(staticMethodsAroundInterceptorClassName, clazz
            .getClassLoader());

        long interceptorTimeCost = 0L;
        long beforeStartTime = System.nanoTime();
        MethodInterceptResult result = new MethodInterceptResult();
        try {
            interceptor.beforeMethod(clazz, method, allArguments, method.getParameterTypes(), result);
        } catch (Throwable t) {
            LOGGER.error(t, "class[{}] before static method[{}] intercept failure", clazz, method.getName());
            AgentSO11Y.recordInterceptorError(pluginName, INTERCEPTOR_TYPE);
        }
        interceptorTimeCost += System.nanoTime() - beforeStartTime;

        Object ret = null;
        try {
            if (!result.isContinue()) {
                ret = result._ret();
            } else {
                ret = zuper.call(allArguments);
            }
        } catch (Throwable t) {
            long handleExceptionStartTime = System.nanoTime();
            try {
                interceptor.handleMethodException(clazz, method, allArguments, method.getParameterTypes(), t);
            } catch (Throwable t2) {
                LOGGER.error(t2, "class[{}] handle static method[{}] exception failure", clazz, method.getName(), t2.getMessage());
                AgentSO11Y.recordInterceptorError(pluginName, INTERCEPTOR_TYPE);
            }
            interceptorTimeCost += System.nanoTime() - handleExceptionStartTime;
            throw t;
        } finally {
            long afterStartTime = System.nanoTime();
            try {
                ret = interceptor.afterMethod(clazz, method, allArguments, method.getParameterTypes(), ret);
            } catch (Throwable t) {
                LOGGER.error(t, "class[{}] after static method[{}] intercept failure:{}", clazz, method.getName(), t.getMessage());
                AgentSO11Y.recordInterceptorError(pluginName, INTERCEPTOR_TYPE);
            }
            interceptorTimeCost += System.nanoTime() - afterStartTime;
        }
        AgentSO11Y.recordInterceptorTimeCost(interceptorTimeCost);

        return ret;
    }
}
