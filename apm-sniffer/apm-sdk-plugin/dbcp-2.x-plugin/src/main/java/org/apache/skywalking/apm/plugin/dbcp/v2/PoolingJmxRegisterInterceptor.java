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

package org.apache.skywalking.apm.plugin.dbcp.v2;

import org.apache.commons.dbcp2.BasicDataSourceMXBean;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link PoolingJmxRegisterInterceptor} intercepted the method of DBCP jmxRegister  register metric monitor.
 */
public class PoolingJmxRegisterInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String METER_NAME = "datasource";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (objInst.getSkyWalkingDynamicField() != null) {
            BasicDataSourceMXBean basicDataSource = (BasicDataSourceMXBean) objInst;
            String tagValue = (String) objInst.getSkyWalkingDynamicField();
            Map<String, Function<BasicDataSourceMXBean, Supplier<Double>>> metricMap = getMetrics();
            metricMap.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(basicDataSource))
                    .tag("name", tagValue).tag("status", key).build());
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }

    private Map<String, Function<BasicDataSourceMXBean, Supplier<Double>>> getMetrics() {
        Map<String, Function<BasicDataSourceMXBean, Supplier<Double>>> metricMap = new HashMap();
        metricMap.put("numActive", (BasicDataSourceMXBean basicDataSource) -> () -> (double) basicDataSource.getNumActive());
        metricMap.put("maxTotal", (BasicDataSourceMXBean basicDataSource) -> () -> (double) basicDataSource.getMaxTotal());
        metricMap.put("numIdle", (BasicDataSourceMXBean basicDataSource) -> () -> (double) (basicDataSource.getNumIdle()));
        metricMap.put("maxWaitMillis", (BasicDataSourceMXBean basicDataSource) -> () -> (double) basicDataSource.getMaxWaitMillis());
        metricMap.put("maxIdle", (BasicDataSourceMXBean basicDataSource) -> () -> (double) basicDataSource.getMaxIdle());
        metricMap.put("minIdle", (BasicDataSourceMXBean basicDataSource) -> () -> (double) basicDataSource.getMinIdle());
        metricMap.put("initialSize", (BasicDataSourceMXBean basicDataSource) -> () -> (double) basicDataSource.getInitialSize());
        return metricMap;
    }
}
