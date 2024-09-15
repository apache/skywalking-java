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

package org.apache.skywalking.apm.agent.core.so11y;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.skywalking.apm.agent.core.meter.Counter;
import org.apache.skywalking.apm.agent.core.meter.Histogram;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;

/**
 * Agent self-observability meters collect through skywalking native protocols
 */
public class AgentSo11y {

    // A map to cache meter obj(s) for plugins. The key is the plugin name.
    private static final Map<String, Counter> ERROR_COUNTER_CACHE = new ConcurrentHashMap<>();

    // Steps of interceptor time cost histogram
    private static final List<Double> TIME_COST_HISTOGRAM_STEPS = Arrays.asList(
        1000d, 10000d, 50000d, 100000d, 300000d, 500000d,
        1000000d, 5000000d, 10000000d, 20000000d, 50000000d, 100000000d
    );

    // context counter
    private static Counter PROPAGATED_CONTEXT_COUNTER;
    private static Counter SAMPLER_CONTEXT_COUNTER;
    private static Counter FINISH_CONTEXT_COUNTER;

    // ignore context counter
    private static Counter PROPAGATED_IGNORE_CONTEXT_COUNTER;
    private static Counter SAMPLER_IGNORE_CONTEXT_COUNTER;
    private static Counter FINISH_IGNORE_CONTEXT_COUNTER;

    // leaked context counter
    private static Counter LEAKED_CONTEXT_COUNTER;
    private static Counter LEAKED_IGNORE_CONTEXT_COUNTER;

    // context perf histogram
    private static Histogram INTERCEPTOR_TIME_COST;

    public static void measureTracingContextCreation(boolean forceSampling, boolean ignoredTracingContext) {
        if (forceSampling) {
            if (ignoredTracingContext) {
                if (PROPAGATED_IGNORE_CONTEXT_COUNTER == null) {
                    PROPAGATED_IGNORE_CONTEXT_COUNTER = MeterFactory
                        .counter("created_ignored_context_counter")
                        .tag("created_by", "propagated")
                        .build();
                }
                PROPAGATED_IGNORE_CONTEXT_COUNTER.increment(1);
            } else {
                if (PROPAGATED_CONTEXT_COUNTER == null) {
                    PROPAGATED_CONTEXT_COUNTER = MeterFactory
                        .counter("created_tracing_context_counter")
                        .tag("created_by", "propagated")
                        .build();
                }
                PROPAGATED_CONTEXT_COUNTER.increment(1);
            }
        } else {
            if (ignoredTracingContext) {
                if (SAMPLER_IGNORE_CONTEXT_COUNTER == null) {
                    SAMPLER_IGNORE_CONTEXT_COUNTER = MeterFactory
                        .counter("created_ignored_context_counter")
                        .tag("created_by", "sampler")
                        .build();
                }
                SAMPLER_IGNORE_CONTEXT_COUNTER.increment(1);
            } else {
                if (SAMPLER_CONTEXT_COUNTER == null) {
                    SAMPLER_CONTEXT_COUNTER = MeterFactory
                        .counter("created_tracing_context_counter")
                        .tag("created_by", "sampler")
                        .build();
                }
                SAMPLER_CONTEXT_COUNTER.increment(1);
            }
        }
    }

    public static void measureTracingContextCompletion(boolean ignoredTracingContext) {
        if (ignoredTracingContext) {
            if (FINISH_IGNORE_CONTEXT_COUNTER == null) {
                FINISH_IGNORE_CONTEXT_COUNTER = MeterFactory.counter("finished_ignored_context_counter").build();
            }
            FINISH_IGNORE_CONTEXT_COUNTER.increment(1);
        } else {
            if (FINISH_CONTEXT_COUNTER == null) {
                FINISH_CONTEXT_COUNTER = MeterFactory.counter("finished_tracing_context_counter").build();
            }
            FINISH_CONTEXT_COUNTER.increment(1);
        }
    }

    public static void measureLeakedTracingContext(boolean ignoredTracingContext) {
        if (ignoredTracingContext) {
            if (LEAKED_IGNORE_CONTEXT_COUNTER == null) {
                LEAKED_IGNORE_CONTEXT_COUNTER = MeterFactory
                    .counter("possible_leaked_context_counter")
                    .tag("source", "ignore")
                    .build();
            }
            LEAKED_IGNORE_CONTEXT_COUNTER.increment(1);
        } else {
            if (LEAKED_CONTEXT_COUNTER == null) {
                LEAKED_CONTEXT_COUNTER = MeterFactory
                    .counter("possible_leaked_context_counter")
                    .tag("source", "tracing")
                    .build();
            }
            LEAKED_CONTEXT_COUNTER.increment(1);
        }
    }

    public static void durationOfInterceptor(double timeCostInNanos) {
        if (INTERCEPTOR_TIME_COST == null) {
            INTERCEPTOR_TIME_COST = MeterFactory
                .histogram("tracing_context_performance")
                .steps(TIME_COST_HISTOGRAM_STEPS)
                .build();
        }
        INTERCEPTOR_TIME_COST.addValue(timeCostInNanos);
    }

    public static void errorOfPlugin(String pluginName, String interType) {
        Counter counter = ERROR_COUNTER_CACHE.computeIfAbsent(pluginName + interType, key -> MeterFactory
            .counter("interceptor_error_counter")
            .tag("plugin_name", pluginName)
            .tag("inter_type", interType)
            .build()
        );
        counter.increment(1);
    }
}
