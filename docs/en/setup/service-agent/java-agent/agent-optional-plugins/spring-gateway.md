# Spring Gateway Plugin

Spring Gateway Plugin only support Spring Gateway 2.x, 3.x and 4.x. It has capabilities to create entry spans for
incoming calls, continue tracing context propagation in Spring Gateway and create exit spans for outgoing calls.

About the filter extension of Gateway, it provides automatically support as much as possible, including GlobalFilter and GatewayFilter
support. However, the filter extension by using `chain.filter(exchange).then(...)` is not able to transparently.

## Supported Auto-Instrument Filters

```java
@Component
public class Filter1 implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(Filter1.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Available trace context
        // For log framework integration(trace context log output) and manual trace context usage.
        String traceId = TraceContext.traceId();
        log.info("available traceId: {}", traceId);

        String segmentId = TraceContext.segmentId();
        log.info("available segmentId: {}", segmentId);

        int spanId = TraceContext.spanId();
        log.info("available spanId: {}", spanId);

        return chain.filter(exchange);
    }
    @Override
    public int getOrder() {
        return -100;
    }
}
```
```java
@Component
public class GatewayFilter1 implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayFilter1.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Available trace context
        log.info("gatewayFilter1 running");
        return chain.filter(exchange);
    }
}
```

## Unsupported Auto-Instrument Filters
Typically, in the following case, you need to read via [Webflux Tracing Assistant APIs](../Application-toolkit-webflux.md) to get the trace context.

```java
@Component
public class UnsupportedFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(UnsupportedFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = TraceContext.traceId();
        // Trace ID is available as it's in the GlobalFilter.
        log.info("available traceId: {}", traceId);

        String segmentId = TraceContext.segmentId();
        // Segment ID is available as it's in the GlobalFilter.
        log.info("available segmentId: {}", segmentId);

        int spanId = TraceContext.spanId();
        // Span ID is available as it's in the GlobalFilter. 
        log.info("available spanId: {}", spanId);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Trace ID/context is not available, N/A in the all following logs.
            // The trace context is not available in the then-closure.
            // Only webflux assistant API can get the trace context.
            String traceId2 = WebFluxSkyWalkingTraceContext.traceId(exchange);
            // Trace ID is not available, N/A in the logs.
            log.info("unavailable in then-closure, available traceId: {} through webflux assistant API", traceId2);

            String segmentId2 = WebFluxSkyWalkingTraceContext.segmentId(exchange);
            // Segment ID is not available, N/A in the logs.
            log.info("unavailable in then-closure, available segmentId: {} through webflux assistant API", segmentId2);

            int spanId2 = WebFluxSkyWalkingTraceContext.spanId(exchange);
            // Span ID is not available, N/A in the logs.
            log.info("unavailable in then-closure, available spanId: {} through webflux assistant API", spanId2);
        }));
    }

    @Override
    public int getOrder() {
        return 10;
    }
}

``` 