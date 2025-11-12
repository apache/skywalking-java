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

/**
 * Extended ConsumerEnhanceRequiredInfo to hold additional information for ExtendedKafkaConsumer
 */

public class ExtendedConsumerEnhanceRequiredInfo {

    private static final String UNKNOWN = "Unknown";

    private String brokerServers = UNKNOWN;
    private String groupId = UNKNOWN;
    private long startTime;

    public void setBrokerServers(String brokerServers) {
        this.brokerServers = brokerServers != null ? brokerServers : UNKNOWN;
    }

    public String getBrokerServers() {
        return brokerServers;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId != null ? groupId : UNKNOWN;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }
}
