/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package test.apache.skywalking.apm.testcase.shenyu.http;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import test.apache.skywalking.apm.testcase.shenyu.http.services.OrderService;
import test.apache.skywalking.apm.testcase.shenyu.http.services.impl.OrderServiceImpl;

@Configuration
public class DubboConfig {

    private ApplicationConfig applicationConfig = new ApplicationConfig("demo");

    private RegistryConfig registryConfig = new RegistryConfig("N/A");

    private ProtocolConfig protocolConfig = new ProtocolConfig("dubbo", 9999);

    @Bean(destroyMethod = "unexport")
    public ServiceConfig<OrderService> service() {
        ServiceConfig<OrderService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setApplication(applicationConfig);
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setInterface(OrderService.class);
        serviceConfig.setRef(new OrderServiceImpl());
        serviceConfig.setTimeout(5000);
        serviceConfig.export();
        return serviceConfig;
    }

    @Bean(destroyMethod = "destroy")
    public ReferenceConfig<OrderService> reference() {
        ReferenceConfig<OrderService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setInterface(OrderService.class);
        referenceConfig.setUrl("dubbo://localhost:9999");
        return referenceConfig;
    }
    
}
