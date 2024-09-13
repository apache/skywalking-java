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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.skywalking.apm.agent.core.meter.Counter;
import org.apache.skywalking.apm.agent.core.meter.Histogram;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;

/**
 * Agent self-observability meters collect through skywalking native protocols
 */
public class AgentSO11Y {

    // context counter
    private static final Counter PROPAGATED_CONTEXT_COUNTER = MeterFactory
        .counter("created_tracing_context_counter").tag("created_by", "propagated").build();
    private static final Counter SAMPLER_CONTEXT_COUNTER = MeterFactory
        .counter("created_tracing_context_counter").tag("created_by", "sampler").build();
    private static final Counter FINISH_CONTEXT_COUNTER = MeterFactory
        .counter("finished_tracing_context_counter").build();

    // ignore context counter
    private static final Counter PROPAGATED_IGNORE_CONTEXT_COUNTER = MeterFactory
        .counter("created_ignored_context_counter").tag("created_by", "propagated").build();
    private static final Counter SAMPLER_IGNORE_CONTEXT_COUNTER = MeterFactory
        .counter("created_ignored_context_counter").tag("created_by", "sampler").build();
    private static final Counter FINISH_IGNORE_CONTEXT_COUNTER = MeterFactory
        .counter("finished_ignored_context_counter").build();

    // leaked context counter
    private static final Counter LEAKED_CONTEXT_COUNTER = MeterFactory
        .counter("possible_leaked_context_counter").tag("source", "tracing").build();
    private static final Counter LEAKED_IGNORE_CONTEXT_COUNTER = MeterFactory
        .counter("possible_leaked_context_counter").tag("source", "ignore").build();

    // context perf histogram
    private static final Histogram INTERCEPTOR_TIME_COST = MeterFactory
        .histogram("tracing_context_performance")
        .steps(Arrays.asList(0.01d, 0.1d, 0.5d, 1d, 3d, 5d, 10d, 50d, 100d, 200d, 500d, 1000d))
        .build();

    // interceptor error counter
    private static final Map<String, Counter> COUNTER_CACHE = new ConcurrentHashMap<>();

    public static void recordTracingContextCreate(boolean forceSampling, boolean ignoredTracingContext) {
        if (forceSampling) {
            if (ignoredTracingContext) {
                PROPAGATED_IGNORE_CONTEXT_COUNTER.increment(1);
            } else {
                PROPAGATED_CONTEXT_COUNTER.increment(1);
            }
        } else {
            if (ignoredTracingContext) {
                SAMPLER_IGNORE_CONTEXT_COUNTER.increment(1);
            } else {
                SAMPLER_CONTEXT_COUNTER.increment(1);
            }
        }
    }

    public static void recordTracingContextFinish(boolean ignoredTracingContext) {
        if (ignoredTracingContext) {
            FINISH_IGNORE_CONTEXT_COUNTER.increment(1);
        } else {
            FINISH_CONTEXT_COUNTER.increment(1);
        }
    }

    public static void recordLeakedTracingContext(boolean ignoredTracingContext) {
        if (ignoredTracingContext) {
            LEAKED_IGNORE_CONTEXT_COUNTER.increment(1);
        } else {
            LEAKED_CONTEXT_COUNTER.increment(1);
        }
    }

    public static void recordInterceptorTimeCost(double timeCostInNanos) {
        INTERCEPTOR_TIME_COST.addValue(timeCostInNanos / 1000000);
    }

    public static void recordInterceptorError(String pluginName, String interType) {
        Counter counter = COUNTER_CACHE.computeIfAbsent(pluginName + interType, key -> MeterFactory
            .counter("interceptor_error_counter")
            .tag("plugin_name", pluginName)
            .tag("inter_type", interType)
            .build()
        );
        counter.increment(1);
    }
}
