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

package org.apache.skywalking.apm.agent.core.conf.dynamic.watcher;

import org.apache.skywalking.apm.agent.core.conf.dynamic.AgentConfigChangeWatcher;

public class TraceSqlParametersWatcher extends AgentConfigChangeWatcher {
    private volatile boolean traceSqlParameters = false;
    
    private volatile boolean dynamicConfigValidate = false;

    public TraceSqlParametersWatcher(String propertyKey) {
        super(propertyKey);
    }

    @Override
    public void notify(ConfigChangeEvent value) {
        if (EventType.DELETE.equals(value.getEventType())) {
            traceSqlParameters = false;
            dynamicConfigValidate = false;
        } else {
            traceSqlParameters = Boolean.parseBoolean(value.getNewValue());
            dynamicConfigValidate = true;
        }
    }

    @Override
    public String value() {
        return Boolean.toString(traceSqlParameters);
    }
    
    public boolean traceSqlParameters(boolean defaultValue) {
        if (dynamicConfigValidate) {
            return traceSqlParameters;
        } else {
            return defaultValue;
        }
    }
    
}
