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

package org.apache.skywalking.apm.plugin.grizzly.workthreadpool;

import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.glassfish.grizzly.threadpool.AbstractThreadPool;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TransportInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String METER_NAME = "thread_pool";
    private static final String METRIC_POOL_NAME_TAG_NAME = "pool_name";
    private static final String THREAD_POOL_NAME = "grizzly_execute_pool";
    private static final String METRIC_TYPE_TAG_NAME = "metric_type";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        GrizzlyExecutorService executorServices = (GrizzlyExecutorService) allArguments[0];
        // reflect get private field just once
        Field poolField = GrizzlyExecutorService.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        AbstractThreadPool abstractThreadPool = (AbstractThreadPool) poolField.get(executorServices);
        ThreadPoolConfig threadPoolConfig = abstractThreadPool.getConfig();
        MeterFactory.gauge(METER_NAME, () -> (double) threadPoolConfig.getCorePoolSize())
                .tag(METRIC_POOL_NAME_TAG_NAME, THREAD_POOL_NAME)
                .tag(METRIC_TYPE_TAG_NAME, "core_pool_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) threadPoolConfig.getMaxPoolSize())
                .tag(METRIC_POOL_NAME_TAG_NAME, THREAD_POOL_NAME)
                .tag(METRIC_TYPE_TAG_NAME, "max_pool_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) abstractThreadPool.getSize())
                .tag(METRIC_POOL_NAME_TAG_NAME, THREAD_POOL_NAME)
                .tag(METRIC_TYPE_TAG_NAME, "pool_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) abstractThreadPool.getQueue().size())
                .tag(METRIC_POOL_NAME_TAG_NAME, THREAD_POOL_NAME)
                .tag(METRIC_TYPE_TAG_NAME, "queue_size")
                .build();
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
