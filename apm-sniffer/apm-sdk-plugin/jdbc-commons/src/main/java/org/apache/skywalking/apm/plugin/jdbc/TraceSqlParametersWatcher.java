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

package org.apache.skywalking.apm.plugin.jdbc;

import org.apache.skywalking.apm.agent.core.conf.dynamic.AgentConfigChangeWatcher;

public class TraceSqlParametersWatcher extends AgentConfigChangeWatcher {
    
    private final boolean defaultValue;
    
    public TraceSqlParametersWatcher(String propertyKey) {
        super(propertyKey);
        defaultValue = JDBCPluginConfig.Plugin.JDBC.TRACE_SQL_PARAMETERS;
    }

    @Override
    public void notify(ConfigChangeEvent value) {
        if (EventType.DELETE.equals(value.getEventType())) {
            JDBCPluginConfig.Plugin.JDBC.TRACE_SQL_PARAMETERS = defaultValue;
        } else {
            JDBCPluginConfig.Plugin.JDBC.TRACE_SQL_PARAMETERS = Boolean.parseBoolean(value.getNewValue());
        }
    }

    @Override
    public String value() {
        return Boolean.toString(JDBCPluginConfig.Plugin.JDBC.TRACE_SQL_PARAMETERS);
    }
    
}
