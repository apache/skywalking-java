/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.testcase.resttemplate;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PreDestroy;
import java.util.List;
import org.apache.skywalking.apm.meter.micrometer.SkywalkingMeterRegistry;
import org.apache.skywalking.apm.meter.micrometer.observation.SkywalkingMeterHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ResttemplateConfiguration {

    @Bean
    public RestTemplate restTemplate(ObservationRegistry observationRegistry) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setObservationRegistry(observationRegistry);
        return restTemplate;
    }

    @Configuration(proxyBeanMethods = false)
    static class ObservationConfiguration {

        @Bean
        ObservationRegistry observationRegistry(List<MeterObservationHandler<?>> handlers) {
            ObservationRegistry registry = ObservationRegistry.create();
            registry.observationConfig().observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(handlers));
            return registry;
        }

        @Bean
        SkywalkingMeterRegistry meterRegistry() {
            return new SkywalkingMeterRegistry();
        }

        @Bean
        MeterObservationHandler<?> meterObservationHandler(SkywalkingMeterRegistry skywalkingMeterRegistry) {
            return new SkywalkingMeterHandler(skywalkingMeterRegistry);
        }

        @Bean
        MetricsDumper metricsDumper(MeterRegistry meterRegistry) {
            return new MetricsDumper(meterRegistry);
        }

        static class MetricsDumper {
            private final MeterRegistry meterRegistry;

            MetricsDumper(MeterRegistry meterRegistry) {
                this.meterRegistry = meterRegistry;
            }

            @PreDestroy
            void dumpMetrics() {
                System.out.println("==== METRICS ====");
                this.meterRegistry.getMeters().forEach(meter -> System.out.println(" - Metric type \t[" + meter.getId().getType() + "],\tname [" + meter.getId().getName() + "],\ttags " + meter.getId().getTags() + ",\tmeasurements " + meter.measure()));
                System.out.println("=================");
            }
        }
    }
}
