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

package test.apache.skywalking.apm.testcase.nacos.client.controller;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Properties;

@RestController
@RequestMapping("/case")
public class CaseController {

    @Value("${nacos.serverAddr}")
    private String nacosServerAddr;

    @RequestMapping("/nacos-client-2.x-case")
    @ResponseBody
    public String nacosClientCase() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("serverAddr", nacosServerAddr);
        ConfigService configService = NacosFactory.createConfigService(prop);
        prop.put(PropertyKeyConst.NAMESPACE, "public");

        NamingService namingService = NamingFactory.createNamingService(prop);
        String serviceName = "nacos-scenario";
        String ip = "1.1.1.1";
        int port = 10000;

        namingService.registerInstance(serviceName, ip, port);

        EventListener listener = event -> {
        };
        namingService.subscribe(serviceName, listener);

        configService.publishConfig(Constants.DATAID, Constants.DEFAULT_GROUP, "content");
        return "Success";
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        return "success";
    }
}

