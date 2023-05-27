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

package org.apache.skywalking.apm.toolkit.webflux;

import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

/**
 * TraceContext for WebFlux.
 */
public class WebFluxSkyWalkingTraceContext {
     /**
     * Try to get the traceId of current trace context.
     *
     * @param serverWebExchange - EnhancedInstance that contains the tracing context
     * @return traceId, if it exists, or empty {@link String}.
     */
    public static String traceId(ServerWebExchange serverWebExchange) {
        return "";
    }

    /**
     * Try to get the segmentId of current trace context.
     *
     * @param serverWebExchange - EnhancedInstance that contains the tracing context
     * @return segmentId, if it exists, or empty {@link String}.
     */
    public static String segmentId(ServerWebExchange serverWebExchange) {
        return "";
    }

    /**
     * Try to get the spanId of current trace context. The spanId is a negative number when the trace context is
     * missing.
     *
     * @param serverWebExchange - EnhancedInstance that contains the tracing context
     * @return spanId, if it exists, or empty {@link String}.
     */
    public static int spanId(ServerWebExchange serverWebExchange) {
        return -1;
    }

    /**
     * Try to get the custom value from trace context.
     *
     * @param serverWebExchange - EnhancedInstance that contains the tracing context
     * @return custom data value.
     */
    public static Optional<String> getCorrelation(ServerWebExchange serverWebExchange, String key) {
        return Optional.empty();
    }

    /**
     * Put the custom key/value into trace context.
     *
     * @param serverWebExchange - EnhancedInstance that contains the tracing context
     * @return previous value if it exists.
     */
    public static Optional<String> putCorrelation(ServerWebExchange serverWebExchange, String key, String value) {
        return Optional.empty();
    }
}
