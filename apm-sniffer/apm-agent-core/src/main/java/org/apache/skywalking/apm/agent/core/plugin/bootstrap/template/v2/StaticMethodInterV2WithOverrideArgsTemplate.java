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

package org.apache.skywalking.apm.agent.core.plugin.bootstrap.template.v2;

import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.skywalking.apm.agent.core.plugin.bootstrap.IBootstrapLog;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.BootstrapInterRuntimeAssist;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.OverrideCallable;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.StaticMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.so11y.bootstrap.BootstrapPluginSo11y;

/**
 * This class wouldn't be loaded in real env. This is a class template for dynamic class generation.
 */
public class StaticMethodInterV2WithOverrideArgsTemplate {

    private static final String INTERCEPTOR_TYPE = "static";
    /**
     * This field is never set in the template, but has value in the runtime.
     */
    private static String PLUGIN_NAME;
    /**
     * This field is never set in the template, but has value in the runtime.
     */
    private static String TARGET_INTERCEPTOR;

    private static StaticMethodsAroundInterceptorV2 INTERCEPTOR;
    private static IBootstrapLog LOGGER;
    private static BootstrapPluginSo11y PLUGIN_SO11Y;

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
    public static Object intercept(@Origin Class<?> clazz, @AllArguments Object[] allArguments, @Origin Method method,
        @Morph OverrideCallable zuper) throws Throwable {
        prepare();

        long interceptorTimeCost = 0L;
        long startTimeOfMethodBeforeInter = System.nanoTime();
        MethodInvocationContext context = new MethodInvocationContext();
        try {
            if (INTERCEPTOR != null) {
                INTERCEPTOR.beforeMethod(clazz, method, allArguments, method.getParameterTypes(), context);
            }
        } catch (Throwable t) {
            LOGGER.error(t, "class[{}] before static method[{}] intercept failure", clazz, method.getName());
            PLUGIN_SO11Y.error(PLUGIN_NAME, INTERCEPTOR_TYPE);
        }
        interceptorTimeCost += System.nanoTime() - startTimeOfMethodBeforeInter;

        Object ret = null;
        try {
            if (!context.isContinue()) {
                ret = context._ret();
            } else {
                ret = zuper.call(allArguments);
            }
        } catch (Throwable t) {
            long startTimeOfMethodHandleExceptionInter = System.nanoTime();
            try {
                if (INTERCEPTOR != null) {
                    INTERCEPTOR.handleMethodException(clazz, method, allArguments, method.getParameterTypes(), t, context);
                }
            } catch (Throwable t2) {
                LOGGER.error(t2, "class[{}] handle static method[{}] exception failure", clazz, method.getName(), t2.getMessage());
                PLUGIN_SO11Y.error(PLUGIN_NAME, INTERCEPTOR_TYPE);
            }
            interceptorTimeCost += System.nanoTime() - startTimeOfMethodHandleExceptionInter;
            throw t;
        } finally {
            long startTimeOfMethodAfterInter = System.nanoTime();
            try {
                if (INTERCEPTOR != null) {
                    ret = INTERCEPTOR.afterMethod(clazz, method, allArguments, method.getParameterTypes(), ret, context);
                }
            } catch (Throwable t) {
                LOGGER.error(t, "class[{}] after static method[{}] intercept failure:{}", clazz, method.getName(), t.getMessage());
                PLUGIN_SO11Y.error(PLUGIN_NAME, INTERCEPTOR_TYPE);
            }
            interceptorTimeCost += System.nanoTime() - startTimeOfMethodAfterInter;
        }
        PLUGIN_SO11Y.duration(interceptorTimeCost);

        return ret;
    }

    /**
     * Prepare the context. Link to the agent core in AppClassLoader.
     */
    private static void prepare() {
        if (INTERCEPTOR == null) {
            ClassLoader loader = BootstrapInterRuntimeAssist.getAgentClassLoader();

            if (loader != null) {
                IBootstrapLog logger = BootstrapInterRuntimeAssist.getLogger(loader, TARGET_INTERCEPTOR);
                if (logger != null) {
                    LOGGER = logger;

                    PLUGIN_SO11Y = BootstrapInterRuntimeAssist.getSO11Y(loader);
                    INTERCEPTOR = BootstrapInterRuntimeAssist.createInterceptor(loader, TARGET_INTERCEPTOR, LOGGER);
                }
            } else {
                LOGGER.error("Runtime ClassLoader not found when create {}." + TARGET_INTERCEPTOR);
            }
        }
    }
}
