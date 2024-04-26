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

package org.apache.skywalking.apm.plugin.c3p0;

import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser.URLParser;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

/**
 * {@link PoolingCreationInterceptor} intercepted the method of pool creation.
 */
public class PoolingCreationInterceptor implements InstanceMethodsAroundInterceptor {
    private static final Set<String> TOKEN_MAP = new HashSet<>(16);

    @Override
    public void beforeMethod(final EnhancedInstance enhancedInstance,
                             final Method method,
                             final Object[] objects,
                             final Class<?>[] classes,
                             final MethodInterceptResult methodInterceptResult) throws Throwable {

    }

    @Override
    public Object afterMethod(final EnhancedInstance enhancedInstance,
                              final Method method,
                              final Object[] objects,
                              final Class<?>[] classes,
                              final Object ret) throws Throwable {
        C3P0Registry.getPooledDataSources().forEach(obj -> {
            ComboPooledDataSource pooledDataSource = (ComboPooledDataSource) obj;
            if (!TOKEN_MAP.contains(pooledDataSource.getIdentityToken())) {
                ConnectionInfo connectionInfo = URLParser.parser(pooledDataSource.getJdbcUrl());
                String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
                Map<String, Function<ComboPooledDataSource, Supplier<Double>>> metricMap = getMetrics();
                metricMap.forEach(
                    (key, value) -> MeterFactory.gauge(PoolConstants.METER_NAME, value.apply(pooledDataSource))
                                                .tag(PoolConstants.METER_TAG_NAME, tagValue)
                                                .tag(PoolConstants.METER_TAG_STATUS, key)
                                                .build());
                TOKEN_MAP.add(pooledDataSource.getIdentityToken());
            }
        });
        return ret;
    }

    @Override
    public void handleMethodException(final EnhancedInstance enhancedInstance,
                                      final Method method,
                                      final Object[] objects,
                                      final Class<?>[] classes,
                                      final Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }

    private Map<String, Function<ComboPooledDataSource, Supplier<Double>>> getMetrics() {
        Map<String, Function<ComboPooledDataSource, Supplier<Double>>> metricMap = new HashMap<>();
        metricMap.put(PoolConstants.NUM_TOTAL_CONNECTIONS, (ComboPooledDataSource pooledDataSource) -> () -> {
            double numConnections = 0;
            try {
                numConnections = pooledDataSource.getNumConnections();
            } catch (SQLException e) {
                ContextManager.activeSpan().errorOccurred().log(e);
            }
            return numConnections;
        });
        metricMap.put(PoolConstants.NUM_BUSY_CONNECTIONS, (ComboPooledDataSource pooledDataSource) -> () -> {
            double numBusyConnections = 0;
            try {
                numBusyConnections = pooledDataSource.getNumBusyConnections();
            } catch (SQLException e) {
                ContextManager.activeSpan().errorOccurred().log(e);
            }
            return numBusyConnections;
        });
        metricMap.put(PoolConstants.NUM_IDLE_CONNECTIONS, (ComboPooledDataSource pooledDataSource) -> () -> {
            double numIdleConnections = 0;
            try {
                numIdleConnections = pooledDataSource.getNumIdleConnections();
            } catch (SQLException e) {
                ContextManager.activeSpan().errorOccurred().log(e);
            }
            return numIdleConnections;
        });
        metricMap.put(
            PoolConstants.MAX_IDLE_TIME,
            (ComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getMaxIdleTime()
        );
        metricMap.put(
            PoolConstants.MIN_POOL_SIZE,
            (ComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getMinPoolSize()
        );
        metricMap.put(
            PoolConstants.MAX_POOL_SIZE,
            (ComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getMaxPoolSize()
        );
        metricMap.put(
            PoolConstants.INITIAL_POOL_SIZE,
            (ComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getInitialPoolSize()
        );
        return metricMap;
    }
}
