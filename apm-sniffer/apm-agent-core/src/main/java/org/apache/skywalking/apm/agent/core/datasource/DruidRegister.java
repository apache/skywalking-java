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

package org.apache.skywalking.apm.agent.core.datasource;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.meter.Counter;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;

import javax.management.ObjectName;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DruidRegister extends AbstractRegister {
    private static final ILog LOGGER = LogManager.getLogger(DruidRegister.class);

    @Override
    public boolean register() {
        try {
            LOGGER.info("DruidRegister register start");
            ObjectName pooledDataSourceObjectName = new ObjectName("com.alibaba.druid:type=DruidDataSource,id=*");
            Set<ObjectName> objectNameSet = mbeanServer.queryNames(pooledDataSourceObjectName, null);

            if (objectNameSet == null || objectNameSet.isEmpty()) {
                return false;
            }

            Map<String, Integer> datasources = new LinkedHashMap<String, Integer>();
            for (ObjectName objectName : objectNameSet) {
                String jdbcUrl = getStringAttribute(objectName, "Url");
                Database datasource = super.parseDatabase(jdbcUrl);
                String key = getConnection(datasources, datasource.toString());
                LOGGER.debug("key {}", key);

                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "ActiveCount").doubleValue()).tag("name", key + "/active").build();
                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "PoolingCount").doubleValue()).tag("name", key + "/total").build();
                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "PoolingCount").doubleValue() - getIntegerAttribute(objectName, "ActiveCount").doubleValue()).tag("name", key + "/idle").build();
                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "LockQueueLength").doubleValue()).tag("name", key + "/lock").build();
                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "MaxWaitThreadCount").doubleValue()).tag("name", key + "/wait").build();
                MeterFactory.gauge("datasource", () -> getLongAttribute(objectName, "CommitCount").doubleValue()).tag("name", key + "/commit").build();
                MeterFactory.gauge("datasource", () -> getLongAttribute(objectName, "ConnectCount").doubleValue()).tag("name", key + "/connect").build();
                MeterFactory.gauge("datasource", () -> getLongAttribute(objectName, "ConnectErrorCount").doubleValue()).tag("name", key + "/connect_error").build();
                MeterFactory.gauge("datasource", () -> getLongAttribute(objectName, "CreateErrorCount").doubleValue()).tag("name", key + "/create_error").build();
            }
            LOGGER.info("DruidRegister register end");
        } catch (Exception e) {
            LOGGER.error("Druid Register fail", e);
        }
        return true;
    }

}
