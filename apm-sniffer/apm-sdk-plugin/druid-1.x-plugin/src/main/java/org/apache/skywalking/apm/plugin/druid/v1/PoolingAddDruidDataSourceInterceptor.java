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

import com.alibaba.druid.pool.DruidDataSourceMBean;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser.URLParser;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link PoolingAddDruidDataSourceInterceptor} intercepted the method of druid addDataSource and register metric monitor.
 */
public class PoolingAddDruidDataSourceInterceptor implements StaticMethodsAroundInterceptor {
    private static final String METER_NAME = "datasource";

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, MethodInterceptResult result) {

    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Object ret) {
        DruidDataSourceMBean druidDataSource = (DruidDataSourceMBean) allArguments[0];
        ConnectionInfo connectionInfo = URLParser.parser(druidDataSource.getUrl());
        String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
        Map<String, Function<DruidDataSourceMBean, Supplier<Double>>> metricMap = getMetrics();
        metricMap.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(druidDataSource))
                .tag("name", tagValue).tag("status", key).build());
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Throwable t) {

    }

    private Map<String, Function<DruidDataSourceMBean, Supplier<Double>>> getMetrics() {
        Map<String, Function<DruidDataSourceMBean, Supplier<Double>>> metricMap = new HashMap();
        metricMap.put("activeCount", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getActiveCount());
        metricMap.put("poolingCount", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getPoolingCount());
        metricMap.put("idleCount", (DruidDataSourceMBean druidDataSource) -> () -> (double) (druidDataSource.getPoolingCount() - druidDataSource.getActiveCount()));
        metricMap.put("lockQueueLength", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getLockQueueLength());
        metricMap.put("maxWaitThreadCount", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getMaxWaitThreadCount());
        metricMap.put("commitCount", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getCommitCount());
        metricMap.put("connectCount", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getConnectCount());
        metricMap.put("connectError", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getConnectErrorCount());
        metricMap.put("createError", (DruidDataSourceMBean druidDataSource) -> () -> (double) druidDataSource.getCreateErrorCount());
        return metricMap;
    }
}
