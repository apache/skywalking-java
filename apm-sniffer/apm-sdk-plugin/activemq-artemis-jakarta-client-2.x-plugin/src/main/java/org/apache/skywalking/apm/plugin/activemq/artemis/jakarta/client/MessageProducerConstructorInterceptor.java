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
 */

package org.apache.skywalking.apm.plugin.activemq.artemis.jakarta.client;

import java.util.Map;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.plugin.activemq.artemis.jakarta.client.define.EnhanceInfo;

/**
 * {@link MessageProducerConstructorInterceptor} get enhance data from the constructor of {@link org.apache.activemq.artemis.jms.client.ActiveMQMessageProducer}
 */
public class MessageProducerConstructorInterceptor implements InstanceConstructorInterceptor {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "61616";
    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";

    @Override
    public void onConstruct(final EnhancedInstance objInst, final Object[] allArguments) throws Throwable {
        ActiveMQConnection connection = (ActiveMQConnection) allArguments[0];
        Map<String, Object> paramMap = connection.getSessionFactory().getConnectorConfiguration().getParams();
        ActiveMQDestination destination = (ActiveMQDestination) allArguments[2];
        EnhanceInfo enhanceInfo = new EnhanceInfo();
        enhanceInfo.setBrokerUrl(paramMap.getOrDefault(HOST_KEY, DEFAULT_HOST) + ":" + paramMap.getOrDefault(PORT_KEY
            , DEFAULT_PORT));
        enhanceInfo.setName(destination.getName());
        enhanceInfo.setAddress(destination.getAddress());
        enhanceInfo.setType(destination.getType());
        objInst.setSkyWalkingDynamicField(enhanceInfo);
    }
}
