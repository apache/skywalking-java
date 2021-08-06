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

package org.apache.skywalking.oap.server.core.analysis.meter;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.skywalking.oap.server.core.UnexpectedException;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.source.DetectPoint;

/**
 * MeterEntity represents the entity in the meter system.
 */
@EqualsAndHashCode
@ToString
@Getter
@Builder(toBuilder = true)
public class MeterEntity {
    private ScopeType scopeType;
    @Setter
    private String serviceName;
    private String instanceName;
    private String endpointName;
    private String sourceServiceName;
    private String destServiceName;
    private DetectPoint detectPoint;

    public String id() {
        switch (scopeType) {
            case SERVICE:
                // In Meter system, only normal service, because we don't conjecture any node.
                return IDManager.ServiceID.buildId(serviceName, true);
            case SERVICE_INSTANCE:
                return IDManager.ServiceInstanceID.buildId(
                    IDManager.ServiceID.buildId(serviceName, true), instanceName);
            case ENDPOINT:
                return IDManager.EndpointID.buildId(IDManager.ServiceID.buildId(serviceName, true), endpointName);
            case SERVICE_RELATION:
                return IDManager.ServiceID.buildRelationId(new IDManager.ServiceID.ServiceRelationDefine(
                    sourceServiceId(),
                    destServiceId()
                ));
            default:
                throw new UnexpectedException("Unexpected scope type of entity " + this.toString());
        }
    }

    public String serviceId() {
        return IDManager.ServiceID.buildId(serviceName, true);
    }

    public String sourceServiceId() {
        return IDManager.ServiceID.buildId(sourceServiceName, true);
    }

    public String destServiceId() {
        return IDManager.ServiceID.buildId(destServiceName, true);
    }

    /**
     * Create a service level meter entity.
     */
    public static MeterEntity newService(String serviceName) {
        return MeterEntity.builder().scopeType(ScopeType.SERVICE).serviceName(serviceName).build();
    }

    /**
     * Create a service instance level meter entity.
     */
    public static MeterEntity newServiceInstance(String serviceName, String serviceInstance) {
        return MeterEntity.builder()
                          .scopeType(ScopeType.SERVICE_INSTANCE)
                          .serviceName(serviceName)
                          .instanceName(serviceInstance)
                          .build();
    }

    /**
     * Create an endpoint level meter entity.
     */
    public static MeterEntity newEndpoint(String serviceName, String endpointName) {
        return MeterEntity.builder()
                          .scopeType(ScopeType.ENDPOINT)
                          .serviceName(serviceName)
                          .endpointName(endpointName)
                          .build();
    }

    public static MeterEntity newServiceRelation(String sourceServiceName,
                                                 String destServiceName,
                                                 DetectPoint detectPoint) {
        return MeterEntity.builder()
                          .scopeType(ScopeType.SERVICE_RELATION)
                          .sourceServiceName(sourceServiceName)
                          .destServiceName(destServiceName)
                          .detectPoint(detectPoint)
                          .build();
    }
}
