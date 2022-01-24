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

package org.apache.skywalking.apm.plugin.undertow.worker.thread.pool;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

public class UndertowWorkerThreadPoolConstructorIntercept implements InstanceConstructorInterceptor {

    private static final String THREAD_POOL_NAME = "undertow_worker_pool";

    private static final Map<String, Function<ThreadPoolExecutor, Supplier<Double>>> METRIC_MAP = ImmutableMap.of(
            "core_pool_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getCorePoolSize(),
            "max_pool_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getMaximumPoolSize(),
            "pool_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getPoolSize(),
            "queue_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getQueue().size(),
            "active_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getActiveCount());

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) throws Throwable {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) objInst;
        buildThreadPoolMeterMetric(threadPoolExecutor);
    }

    private void buildThreadPoolMeterMetric(ThreadPoolExecutor threadPoolExecutor) {
        String threadPoolMeterName = "thread_pool";
        String poolNameTag = "pool_name";
        String metricTypeTag = "metric_type";
        METRIC_MAP.forEach((key, value) -> MeterFactory.gauge(threadPoolMeterName, value.apply(threadPoolExecutor))
                .tag(poolNameTag, THREAD_POOL_NAME).tag(metricTypeTag, key).build());
    }
}
