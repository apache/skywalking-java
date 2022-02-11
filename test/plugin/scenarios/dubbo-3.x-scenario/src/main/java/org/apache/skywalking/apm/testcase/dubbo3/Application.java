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

package org.apache.skywalking.apm.testcase.dubbo3;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.skywalking.apm.testcase.dubbo3.services.ExceptionService;
import org.apache.skywalking.apm.testcase.dubbo3.services.GreetService;
import org.apache.skywalking.apm.testcase.dubbo3.services.impl.ExceptionServiceImpl;
import org.apache.skywalking.apm.testcase.dubbo3.services.impl.GreetServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        new EmbeddedZooKeeper(2181, false).start();
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    public static class DubboConfiguration {

        private ApplicationConfig applicationConfig = new ApplicationConfig(Application.class.getSimpleName());

        private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");

        private ProtocolConfig protocolConfig = new ProtocolConfig("dubbo", 20080);

        public DubboConfiguration() {
            ApplicationModel.getConfigManager().setApplication(applicationConfig);
        }

        @Bean(destroyMethod = "unexport")
        public ServiceConfig<GreetService> service() {
            ServiceConfig<GreetService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setRegistry(registryConfig);
            serviceConfig.setProtocol(protocolConfig);
            serviceConfig.setInterface(GreetService.class);
            serviceConfig.setRef(new GreetServiceImpl());
            serviceConfig.setTimeout(5000);
            serviceConfig.export();
            return serviceConfig;
        }

        @Bean(destroyMethod = "unexport")
        public ServiceConfig<ExceptionService> service2() {
            ServiceConfig<ExceptionService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setApplication(applicationConfig);
            serviceConfig.setRegistry(registryConfig);
            serviceConfig.setProtocol(protocolConfig);
            serviceConfig.setInterface(ExceptionService.class);
            serviceConfig.setRef(new ExceptionServiceImpl());
            serviceConfig.setTimeout(500000);
            serviceConfig.export();
            return serviceConfig;
        }

        @Bean(destroyMethod = "destroy")
        public ReferenceConfig<GreetService> reference() {
            ReferenceConfig<GreetService> referenceConfig = new ReferenceConfig<>();
            referenceConfig.setRegistry(registryConfig);
            referenceConfig.setInterface(GreetService.class);
            referenceConfig.setScope("remote");
            return referenceConfig;
        }
    }

}
