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
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;

import javax.management.ObjectName;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class HikaricpRegister extends AbstractRegister {
    private static final ILog LOGGER = LogManager.getLogger(HikaricpRegister.class);

    @Override
    public boolean register() {
        try {
            LOGGER.info("HikariCPRegister register start");

            ObjectName pooledDataSourceObjectName = new ObjectName("com.zaxxer.hikari:type=Pool (*)");
            Set<ObjectName> objectNameSet = mbeanServer.queryNames(pooledDataSourceObjectName, null);

            if (objectNameSet == null || objectNameSet.isEmpty()) {
                return false;
            }
            Map<String, Integer> datasources = new LinkedHashMap<String, Integer>();

            for (ObjectName objectName : objectNameSet) {
                String jdbcUrl = getStringAttribute(objectName, "Url");
                LOGGER.info("jdbcUrl", jdbcUrl);
                Database datasource = super.parseDatabase(jdbcUrl);
                String key = getConnection(datasources, datasource.toString());

                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "activeConnections").doubleValue()).tag("name", key + "/active").build();
                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "totalConnections").doubleValue()).tag("name", key + "/total").build();
                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "idleConnections").doubleValue()).tag("name", key + "/idle").build();
                MeterFactory.gauge("datasource", () -> getIntegerAttribute(objectName, "threadsAwaitingConnection").doubleValue()).tag("name", key + "/wait").build();

            }
            LOGGER.info("HikariCPRegister register end");
        } catch (Exception e) {
            LOGGER.error("HikariCP Register fail", e);
        }
        return true;
    }

}
