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

import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser.URLParser;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link HikariDataSourceConstructorInterceptor} intercepted the method of HikariData construct.
 */
public class HikariDataSourceConstructorInterceptor implements InstanceConstructorInterceptor {
    private static final String METER_NAME = "datasource";
    private static final ILog LOGGER = LogManager.getLogger(HikariDataSourceConstructorInterceptor.class);

    private static final Map<String, Function<HikariPoolMXBean, Supplier<Double>>> METRIC_MAP = ImmutableMap.of(
            "activeConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getActiveConnections(),
            "totalConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getTotalConnections(),
            "idleConnections", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) (hikariPoolMXBean.getIdleConnections()),
            "threadsAwaitingConnection", (HikariPoolMXBean hikariPoolMXBean) -> () -> (double) hikariPoolMXBean.getThreadsAwaitingConnection());

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) throws Throwable {
        if (LOGGER.isInfoEnable()) {
            LOGGER.debug("metric hikari init");
        }
        HikariPoolMXBean hikariDataSource = (HikariPoolMXBean) objInst;
        HikariConfig hikariConfig = (HikariConfig) allArguments[0];
        ConnectionInfo connectionInfo = URLParser.parser(hikariConfig.getJdbcUrl());
        String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
        METRIC_MAP.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(hikariDataSource))
                .tag("name", tagValue).tag("status", key).build());
    }
}
