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

package org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.loader.InterceptorInstanceLoader;
import org.apache.skywalking.apm.agent.core.so11y.AgentSo11y;

/**
 * The actual byte-buddy's interceptor to intercept class instance methods. In this class, it provides a bridge between
 * byte-buddy and sky-walking plugin.
 */
public class StaticMethodsInterV2 {
    private static final ILog LOGGER = LogManager.getLogger(StaticMethodsInterV2.class);

    private static final String INTERCEPTOR_TYPE = "static";

    private String pluginName;
    /**
     * A class full name, and instanceof {@link StaticMethodsAroundInterceptorV2} This name should only stay in {@link
     * String}, the real {@link Class} type will trigger classloader failure. If you want to know more, please check on
     * books about Classloader or Classloader appointment mechanism.
     */
    private String staticMethodsAroundInterceptorClassName;

    /**
     * Set the name of {@link StaticMethodsInterV2#staticMethodsAroundInterceptorClassName}
     *
     * @param staticMethodsAroundInterceptorClassName class full name.
     */
    public StaticMethodsInterV2(String pluginName, String staticMethodsAroundInterceptorClassName) {
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
        @SuperCall Callable<?> zuper) throws Throwable {
        StaticMethodsAroundInterceptorV2 interceptor = InterceptorInstanceLoader.load(staticMethodsAroundInterceptorClassName,
                                                                                      clazz.getClassLoader());

        long interceptorTimeCost = 0L;
        long startTimeOfMethodBeforeInter = System.nanoTime();
        MethodInvocationContext context = new MethodInvocationContext();
        try {
            interceptor.beforeMethod(clazz, method, allArguments, method.getParameterTypes(), context);
        } catch (Throwable t) {
            LOGGER.error(t, "class[{}] before static method[{}] intercept failure", clazz, method.getName());
            AgentSo11y.errorOfPlugin(pluginName, INTERCEPTOR_TYPE);
        }
        interceptorTimeCost += System.nanoTime() - startTimeOfMethodBeforeInter;

        Object ret = null;
        try {
            if (!context.isContinue()) {
                ret = context._ret();
            } else {
                ret = zuper.call();
            }
        } catch (Throwable t) {
            long startTimeOfMethodHandleExceptionInter = System.nanoTime();
            try {
                interceptor.handleMethodException(clazz, method, allArguments, method.getParameterTypes(), t, context);
            } catch (Throwable t2) {
                LOGGER.error(t2, "class[{}] handle static method[{}] exception failure", clazz, method.getName(), t2.getMessage());
                AgentSo11y.errorOfPlugin(pluginName, INTERCEPTOR_TYPE);
            }
            interceptorTimeCost += System.nanoTime() - startTimeOfMethodHandleExceptionInter;
            throw t;
        } finally {
            long startTimeOfMethodAfterInter = System.nanoTime();
            try {
                ret = interceptor.afterMethod(clazz, method, allArguments, method.getParameterTypes(), ret, context);
            } catch (Throwable t) {
                LOGGER.error(t, "class[{}] after static method[{}] intercept failure:{}", clazz, method.getName(), t.getMessage());
                AgentSo11y.errorOfPlugin(pluginName, INTERCEPTOR_TYPE);
            }
            interceptorTimeCost += System.nanoTime() - startTimeOfMethodAfterInter;
        }
        AgentSo11y.durationOfInterceptor(interceptorTimeCost);

        return ret;
    }
}
