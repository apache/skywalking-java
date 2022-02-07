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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
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
 * {@link PoolingSealInterceptor} intercepted the method of HikariCP getting connection.
 */
public class PoolingSealInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog LOGGER = LogManager.getLogger(PoolingSealInterceptor.class);
    private static final String METER_NAME = "datasource";
    private static final Map<String, Function<HikariPoolMXBean, Supplier<Double>>> METRIC_CONFIG_MAP = new HashMap<String, Function<HikariPoolMXBean, Supplier<Double>>>();
    private static final Map<String, Function<HikariConfig, Supplier<Double>>> METRIC_MAP = new HashMap<String, Function<HikariConfig, Supplier<Double>>>();

    static {
        METRIC_CONFIG_MAP.put("activeConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getActiveConnections());
        METRIC_CONFIG_MAP.put("totalConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getTotalConnections());
        METRIC_CONFIG_MAP.put("idleConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getIdleConnections());
        METRIC_CONFIG_MAP.put("threadsAwaitingConnection", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getThreadsAwaitingConnection());
        METRIC_MAP.put("connectionTimeout", (HikariConfig hikariConfig) -> () -> (double) hikariConfig.getConnectionTimeout());
        METRIC_MAP.put("validationTimeout", (HikariConfig hikariConfig) -> () -> (double) hikariConfig.getValidationTimeout());
        METRIC_MAP.put("idleTimeout", (HikariConfig hikariConfig) -> () -> (double) hikariConfig.getIdleTimeout());
        METRIC_MAP.put("leakDetectionThreshold", (HikariConfig hikariConfig) -> () -> (double) hikariConfig.getLeakDetectionThreshold());
        METRIC_MAP.put("minimumIdle", (HikariConfig hikariConfig) -> () -> (double) hikariConfig.getMinimumIdle());
        METRIC_MAP.put("maximumPoolSize", (HikariConfig hikariConfig) -> () -> (double) hikariConfig.getMaximumPoolSize());
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (LOGGER.isInfoEnable()) {
            LOGGER.info("metric hikari init");
        }

        HikariDataSource hikariDataSource = (HikariDataSource) objInst;
        ConnectionInfo connectionInfo = URLParser.parser(hikariDataSource.getJdbcUrl());
        String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
        METRIC_CONFIG_MAP.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(hikariDataSource.getHikariPoolMXBean()))
                .tag("name", tagValue).tag("status", key).build());
        METRIC_MAP.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(hikariDataSource))
                .tag("name", tagValue).tag("status", key).build());
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
