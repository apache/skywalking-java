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

package org.apache.skywalking.apm.plugin.jdbc.clickhouse;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

/**
 * Enhance {@link ru.yandex.clickhouse.ClickHouseConnectionImpl#initConnection(ClickHouseProperties)} method.
 * <p>
 * ClickHouse JDBC Driver uses this method to simulate the action of connecting database in JDBC protocol.
 * So this method is enhanced to prevent the generation of exit span of http type.
 * </p>
 * <p>
 * This interceptor is used to replace {@link org.apache.skywalking.apm.plugin.jdbc.JDBCDriverInterceptor}.
 * </p>
 */
public class InitConnectionMethodInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            MethodInterceptResult result) throws Throwable {
        final Object properties = allArguments[0];
        ClickHouseProperties clickHouseProperties = (ClickHouseProperties) properties;
        final ConnectionInfo connectionInfo = new ConnectionInfo(ComponentsDefine.CLICKHOUSE_JDBC_DRIVER,
                "ClickHouse", clickHouseProperties.getHost(), clickHouseProperties.getPort(),
                clickHouseProperties.getDatabase());
        objInst.setSkyWalkingDynamicField(connectionInfo);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Throwable t) {

    }
}
