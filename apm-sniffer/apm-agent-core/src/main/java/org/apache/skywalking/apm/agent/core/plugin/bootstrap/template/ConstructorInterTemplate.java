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

package org.apache.skywalking.apm.agent.core.plugin.bootstrap.template;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.skywalking.apm.agent.core.plugin.bootstrap.IBootstrapLog;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.BootstrapInterRuntimeAssist;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.so11y.bootstrap.BootstrapPluginSo11y;

/**
 * --------CLASS TEMPLATE---------
 * <p>Author, Wu Sheng </p>
 * <p>Comment, don't change this unless you are 100% sure the agent core mechanism for bootstrap class
 * instrumentation.</p>
 * <p>Date, 24th July 2019</p>
 * -------------------------------
 * <p>
 * This class wouldn't be loaded in real env. This is a class template for dynamic class generation.
 */
public class ConstructorInterTemplate {

    private static final String INTERCEPTOR_TYPE = "constructor";
    /**
     * This field is never set in the template, but has value in the runtime.
     */
    private static String PLUGIN_NAME;
    /**
     * This field is never set in the template, but has value in the runtime.
     */
    private static String TARGET_INTERCEPTOR;

    private static InstanceConstructorInterceptor INTERCEPTOR;
    private static IBootstrapLog LOGGER;
    private static BootstrapPluginSo11y PLUGIN_SO11Y;

    /**
     * Intercept the target constructor.
     *
     * @param obj          target class instance.
     * @param allArguments all constructor arguments
     */
    @RuntimeType
    public static void intercept(@This Object obj, @AllArguments Object[] allArguments) {
        long interceptorTimeCost = 0L;
        long startTime = System.nanoTime();
        try {
            prepare();

            EnhancedInstance targetObject = (EnhancedInstance) obj;

            if (INTERCEPTOR == null) {
                return;
            }
            INTERCEPTOR.onConstruct(targetObject, allArguments);
        } catch (Throwable t) {
            LOGGER.error("ConstructorInter failure.", t);
            PLUGIN_SO11Y.error(PLUGIN_NAME, INTERCEPTOR_TYPE);
        }
        interceptorTimeCost += System.nanoTime() - startTime;
        PLUGIN_SO11Y.duration(interceptorTimeCost);
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
