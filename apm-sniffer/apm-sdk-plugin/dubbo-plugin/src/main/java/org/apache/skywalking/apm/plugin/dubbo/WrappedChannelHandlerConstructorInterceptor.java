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

package org.apache.skywalking.apm.plugin.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.store.DataStore;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class WrappedChannelHandlerConstructorInterceptor implements InstanceConstructorInterceptor {
    private static final String METER_NAME = "thread_pool";
    private static final String METRIC_POOL_NAME_TAG_NAME = "pool_name";
    private static final String METRIC_TYPE_TAG_NAME = "metric_type";

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) throws Throwable {
        URL url = (URL) allArguments[1];

        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        String port = Integer.toString(url.getPort());

        final String componentKey = ExecutorService.class.getName();
        ExecutorService executor = (ExecutorService) dataStore.get(componentKey, port);
        if (!(executor instanceof ThreadPoolExecutor)) {
            return;
        }
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        String threadPoolName = String.format("dubbo-provider-%s", port);

        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getCorePoolSize()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "core_pool_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getMaximumPoolSize()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "max_pool_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getLargestPoolSize()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "largest_pool_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getPoolSize()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "pool_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getQueue().size()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "queue_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getActiveCount()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "active_size")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getTaskCount()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "task_count")
                .build();
        MeterFactory.gauge(METER_NAME, () -> (double) (threadPoolExecutor.getCompletedTaskCount()))
                .tag(METRIC_POOL_NAME_TAG_NAME, threadPoolName)
                .tag(METRIC_TYPE_TAG_NAME, "completed_task_count")
                .build();
    }
}
