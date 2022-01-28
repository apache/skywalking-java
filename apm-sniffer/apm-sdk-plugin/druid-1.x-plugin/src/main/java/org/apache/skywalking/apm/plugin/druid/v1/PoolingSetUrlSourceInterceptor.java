/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.druid.v1;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser.URLParser;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link PoolingSetUrlSourceInterceptor} intercepted the method of Druid set url and register metric monitor.
 */
public class PoolingSetUrlSourceInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String METER_NAME = "datasource";
    private static final ILog LOGGER = LogManager.getLogger(PoolingSetUrlSourceInterceptor.class);

    private static final Map<String, Function<DruidDataSource, Supplier<Double>>> METRIC_MAP = new HashMap<String, Function<DruidDataSource, Supplier<Double>>>();

    static {
        METRIC_MAP.put("activeCount", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getActiveCount());
        METRIC_MAP.put("poolingCount", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getPoolingCount());
        METRIC_MAP.put("idleCount", (DruidDataSource druidDataSource) -> () -> (double) (druidDataSource.getPoolingCount() - druidDataSource.getActiveCount()));
        METRIC_MAP.put("lockQueueLength", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getLockQueueLength());
        METRIC_MAP.put("maxWaitThreadCount", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getMaxWaitThreadCount());
        METRIC_MAP.put("commitCount", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getCommitCount());
        METRIC_MAP.put("connectCount", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getConnectCount());
        METRIC_MAP.put("connectError", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getConnectErrorCount());
        METRIC_MAP.put("createError", (DruidDataSource druidDataSource) -> () -> (double) druidDataSource.getCreateErrorCount());
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (LOGGER.isInfoEnable()) {
            LOGGER.info("metric druid init");
        }
        DruidDataSource druidDataSource = (DruidDataSource) objInst;
        ConnectionInfo connectionInfo = URLParser.parser((String) allArguments[0]);
        String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
        METRIC_MAP.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(druidDataSource))
                .tag("name", tagValue).tag("status", key).build());
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
