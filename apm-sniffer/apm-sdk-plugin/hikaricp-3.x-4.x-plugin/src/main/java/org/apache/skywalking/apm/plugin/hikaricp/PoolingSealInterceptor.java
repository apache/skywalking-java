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

package org.apache.skywalking.apm.plugin.hikaricp;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
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
 * {@link PoolingSealInterceptor} intercepted the method of HikariCP getting connection.
 */
public class PoolingSealInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String METER_NAME = "datasource";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {

        HikariDataSource hikariDataSource = (HikariDataSource) objInst;
        ConnectionInfo connectionInfo = URLParser.parser(hikariDataSource.getJdbcUrl());
        String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
        final Map<String, Function<HikariPoolMXBean, Supplier<Double>>> poolMetricMap = getPoolMetrics();
        final Map<String, Function<HikariConfigMXBean, Supplier<Double>>> metricConfigMap = getConfigMetrics();
        poolMetricMap.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(hikariDataSource.getHikariPoolMXBean()))
                .tag("name", tagValue).tag("status", key).build());
        metricConfigMap.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(hikariDataSource))
                .tag("name", tagValue).tag("status", key).build());
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }

    private Map<String, Function<HikariPoolMXBean, Supplier<Double>>> getPoolMetrics() {
        final Map<String, Function<HikariPoolMXBean, Supplier<Double>>> poolMetricMap = new HashMap();
        poolMetricMap.put("activeConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getActiveConnections());
        poolMetricMap.put("totalConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getTotalConnections());
        poolMetricMap.put("idleConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getIdleConnections());
        poolMetricMap.put("threadsAwaitingConnection", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getThreadsAwaitingConnection());
        return poolMetricMap;
    }

    private Map<String, Function<HikariConfigMXBean, Supplier<Double>>> getConfigMetrics() {
        final Map<String, Function<HikariConfigMXBean, Supplier<Double>>> metricConfigMap = new HashMap();
        metricConfigMap.put("connectionTimeout", (HikariConfigMXBean hikariConfigMXBean) -> () -> (double) hikariConfigMXBean.getConnectionTimeout());
        metricConfigMap.put("validationTimeout", (HikariConfigMXBean hikariConfigMXBean) -> () -> (double) hikariConfigMXBean.getValidationTimeout());
        metricConfigMap.put("idleTimeout", (HikariConfigMXBean hikariConfigMXBean) -> () -> (double) hikariConfigMXBean.getIdleTimeout());
        metricConfigMap.put("leakDetectionThreshold", (HikariConfigMXBean hikariConfigMXBean) -> () -> (double) hikariConfigMXBean.getLeakDetectionThreshold());
        metricConfigMap.put("minimumIdle", (HikariConfigMXBean hikariConfigMXBean) -> () -> (double) hikariConfigMXBean.getMinimumIdle());
        metricConfigMap.put("maximumPoolSize", (HikariConfigMXBean hikariConfigMXBean) -> () -> (double) hikariConfigMXBean.getMaximumPoolSize());
        return metricConfigMap;
    }
}
