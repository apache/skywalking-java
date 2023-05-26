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
