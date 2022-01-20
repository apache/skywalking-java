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

package org.apache.skywalking.apm.agent.core.theadpool;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.util.RunnableWithExceptionProtection;

public class ThreadPoolService implements BootService, Runnable {

    private static final ILog LOGGER = LogManager.getLogger(ThreadPoolService.class);

    private volatile ScheduledFuture<?> buildMeterMetricFuture;

    private static final Map<String, Function<ThreadPoolExecutor, Supplier<Double>>> METRIC_MAP = ImmutableMap.of(
            "core_pool_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> {
                return (double) threadPoolExecutor.getCorePoolSize();
            },
            "max_pool_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getMaximumPoolSize(),
            "pool_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getPoolSize(),
            "queue_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getQueue().size(),
            "active_size",
            (ThreadPoolExecutor threadPoolExecutor) -> () -> (double) threadPoolExecutor.getActiveCount());

    @Override
    public void prepare() throws Throwable {
    }

    @Override
    public void boot() throws Throwable {
        buildMeterMetricFuture = Executors.newSingleThreadScheduledExecutor(
                        new DefaultNamedThreadFactory("ThreadPoolService-BuildMetric"))
                .scheduleAtFixedRate(new RunnableWithExceptionProtection(
                        this, t -> LOGGER.error("ThreadPoolService-BuildMetric failure.", t)
                ), 0, 1, TimeUnit.SECONDS);

    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        buildMeterMetricFuture.cancel(true);
    }

    private void buildThreadPoolMeterMetric() {
        String threadPoolMeterName = "thread_pool";
        String poolNameTag = "pool_name";
        String metricTypeTag = "metric_type";
        List<ThreadPoolSourceData> threadPoolSourceDataList = ThreadPoolRegister.getThreadPoolSourceDataList();
        threadPoolSourceDataList.forEach(it -> {
            if (!it.getHasBuildMetric()) {
                String poolName = it.getThreadPoolName().name().toLowerCase();
                ThreadPoolExecutor threadPoolExecutor = it.getThreadPoolExecutor();
                METRIC_MAP.forEach(
                        (key, value) -> MeterFactory.gauge(threadPoolMeterName, value.apply(threadPoolExecutor))
                                .tag(poolNameTag, poolName).tag(metricTypeTag, key).build());
                it.setHasBuildMetric(true);
            }
        });
    }

    @Override
    public void run() {
        buildThreadPoolMeterMetric();
    }
}
