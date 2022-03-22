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

package org.apache.skywalking.apm.testcase.servicecomb;

import org.apache.servicecomb.registry.RegistrationManager;
import org.apache.servicecomb.springboot2.starter.EnableServiceComb;
import org.apache.skywalking.apm.testcase.servicecomb.consumer.CodeFirstPojoConsumerHelloImpl;
import org.apache.skywalking.apm.testcase.servicecomb.provider.CodeFirstPojoHelloProviderImpl;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@EnableServiceComb
public class ConsumerApplication {
    public static void main(String[] args) throws Exception {
        try {
            new SpringApplicationBuilder().web(WebApplicationType.NONE).sources(ConsumerApplication.class).run(args);
           /* registerSchema();
            RestTemplate template = RestTemplateBuilder.create();
            String str = template
                .getForObject("cse://codefirsttest_server/servicecomb/case",
                    String.class);
            System.out.println(str);*/
            /*Log4jUtils.init();
            BeanUtils.init();*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void registerSchema(){
        RegistrationManager.INSTANCE.getSwaggerLoader().registerSwagger("codefirst",
            "codefirsttest_server",
            "codeFirstSpringmvcHelloClient", CodeFirstPojoConsumerHelloImpl.class);
        RegistrationManager.INSTANCE.getSwaggerLoader().registerSwagger("codefirst",
            "codefirsttest_server",
            "CodeFirstPojoHelloProviderImpl", CodeFirstPojoHelloProviderImpl.class);
    }
}