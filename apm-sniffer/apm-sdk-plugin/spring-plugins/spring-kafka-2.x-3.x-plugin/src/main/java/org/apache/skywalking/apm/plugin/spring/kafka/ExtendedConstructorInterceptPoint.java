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

package org.apache.skywalking.apm.plugin.spring.kafka;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.util.StringUtil;

public class ExtendedConstructorInterceptPoint implements InstanceConstructorInterceptor {
    @Override
    public void onConstruct(final EnhancedInstance objInst, final Object[] allArguments) throws Throwable {
        ExtendedConsumerEnhanceRequiredInfo requiredInfo = new ExtendedConsumerEnhanceRequiredInfo();
        extractConsumerConfig(allArguments, requiredInfo);
        objInst.setSkyWalkingDynamicField(requiredInfo);
    }

    private void extractConsumerConfig(Object[] allArguments, ExtendedConsumerEnhanceRequiredInfo requiredInfo) {
        if (allArguments == null || allArguments.length == 0) {
            return;
        }

        for (Object arg : allArguments) {
            if (arg instanceof java.util.Map) {
                extractConfigFromMap(arg, requiredInfo);
                break;
            }
        }
    }

    private void extractConfigFromMap(Object arg, ExtendedConsumerEnhanceRequiredInfo requiredInfo) {
        try {
            java.util.Map<String, Object> configMap = (java.util.Map<String, Object>) arg;
            Object bootstrapServers = configMap.get("bootstrap.servers");
            if (bootstrapServers instanceof java.util.List) {
                requiredInfo.setBrokerServers(StringUtil.join(';', String.valueOf(bootstrapServers)));
            } else if (bootstrapServers != null) {
                requiredInfo.setBrokerServers(bootstrapServers.toString());
            }

            Object groupId = configMap.get("group.id");
            if (groupId != null) {
                requiredInfo.setGroupId(groupId.toString());
            }
        } catch (Exception e) {
            // Ignore exception and continue
        }
    }

}
