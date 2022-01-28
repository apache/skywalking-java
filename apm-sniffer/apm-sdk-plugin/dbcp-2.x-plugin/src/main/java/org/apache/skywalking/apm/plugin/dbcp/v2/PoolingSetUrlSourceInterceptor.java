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

import org.apache.commons.dbcp2.BasicDataSource;
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
 * {@link PoolingSetUrlSourceInterceptor} intercepted the method of DBCP set url and register metric monitor.
 */
public class PoolingSetUrlSourceInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String METER_NAME = "datasource";
    private static final ILog LOGGER = LogManager.getLogger(PoolingSetUrlSourceInterceptor.class);

    private static final Map<String, Function<BasicDataSource, Supplier<Double>>> METRIC_MAP = new HashMap<String, Function<BasicDataSource, Supplier<Double>>>();

    static {
        METRIC_MAP.put("numActive", (BasicDataSource basicDataSource) -> () -> (double) basicDataSource.getNumActive());
        METRIC_MAP.put("maxTotal", (BasicDataSource basicDataSource) -> () -> (double) basicDataSource.getMaxTotal());
        METRIC_MAP.put("numIdle", (BasicDataSource basicDataSource) -> () -> (double) (basicDataSource.getNumIdle()));
        METRIC_MAP.put("maxWaitMillis", (BasicDataSource basicDataSource) -> () -> (double) basicDataSource.getMaxWaitMillis());
        METRIC_MAP.put("maxIdle", (BasicDataSource basicDataSource) -> () -> (double) basicDataSource.getMaxIdle());
        METRIC_MAP.put("minIdle", (BasicDataSource basicDataSource) -> () -> (double) basicDataSource.getMinIdle());
        METRIC_MAP.put("initialSize", (BasicDataSource basicDataSource) -> () -> (double) basicDataSource.getInitialSize());
    }


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (LOGGER.isInfoEnable()) {
            LOGGER.info("metric dbcp init");
        }
        BasicDataSource basicDataSource = (BasicDataSource) objInst;
        ConnectionInfo connectionInfo = URLParser.parser((String) allArguments[0]);
        String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
        METRIC_MAP.forEach((key, value) -> MeterFactory.gauge(METER_NAME, value.apply(basicDataSource))
                .tag("name", tagValue).tag("status", key).build());
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
