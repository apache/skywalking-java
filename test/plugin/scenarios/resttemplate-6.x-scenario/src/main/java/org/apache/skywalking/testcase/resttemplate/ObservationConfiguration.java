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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.skywalking.apm.meter.micrometer.SkywalkingMeterRegistry;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingDefaultTracingHandler;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingMeterHandler;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingReceiverTracingHandler;
import org.apache.skywalking.apm.toolkit.micrometer.observation.SkywalkingSenderTracingHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ObservationConfiguration {
    private static final Logger LOGGER = LogManager.getLogger(ObservationConfiguration.class);

    @Bean
    ObservationRegistry observationRegistry(List<MeterObservationHandler<?>> handlers) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig()
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(handlers));
        registry.observationConfig()
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                    new SkywalkingSenderTracingHandler(), new SkywalkingReceiverTracingHandler(),
                    new SkywalkingDefaultTracingHandler()
                ));
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
            LOGGER.info("==== METRICS ====");
            this.meterRegistry.getMeters()
                              .forEach(meter -> LOGGER.info(
                                  " - Metric type \t[" + meter.getId().getType() + "],\tname [" + meter.getId()
                                                                                                       .getName() + "],\ttags " + meter.getId()
                                                                                                                                       .getTags() + ",\tmeasurements " + meter.measure()));
            LOGGER.info("=================");
        }
    }
}
