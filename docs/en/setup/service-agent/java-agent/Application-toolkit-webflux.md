# Webflux Tracing Assistant APIs

These APIs provide advanced features to enhance interaction capabilities in Webflux cases.

## Choose the right toolkit

The toolkit is split by Reactor generation, because `reactor.core.publisher.Signal#getContext()` was
removed in Reactor 3.5.0 and its replacement, `Signal#getContextView()`, does not exist before
Reactor 3.4.0. No single artifact can serve both.

| Artifact | Java package | Reactor | Spring Boot |
|---|---|---|---|
| `apm-toolkit-webflux-5.x` | `org.apache.skywalking.apm.toolkit.webflux.v5` | 3.1.3 -> 3.4 | 2.x |
| `apm-toolkit-webflux-6.x` | `org.apache.skywalking.apm.toolkit.webflux.v6` | 3.5 -> 3.8 | 3.x and 4.x |

The two artifacts expose exactly the same API, so only the dependency coordinate and the import
change. Add **one** of them — never both.

For Spring Boot 2.x (Reactor 3.1.3 - 3.4):
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-webflux-5.x</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```
```java
import org.apache.skywalking.apm.toolkit.webflux.v5.WebFluxSkyWalkingOperators;
import org.apache.skywalking.apm.toolkit.webflux.v5.WebFluxSkyWalkingTraceContext;
```

For Spring Boot 3.x and 4.x (Reactor 3.5+):
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-webflux-6.x</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```
```java
import org.apache.skywalking.apm.toolkit.webflux.v6.WebFluxSkyWalkingOperators;
import org.apache.skywalking.apm.toolkit.webflux.v6.WebFluxSkyWalkingTraceContext;
```

### Migrating from `apm-toolkit-webflux`

Before 9.8.0 there was a single un-versioned `apm-toolkit-webflux` artifact, in package
`org.apache.skywalking.apm.toolkit.webflux`. It is superseded by `apm-toolkit-webflux-5.x`, and its
`continueTracing` overloads that read the Reactor `Signal` context never worked on Reactor 3.5+
(Spring Boot 3.0 and later).

To migrate, change the artifactId and the import: pick `-5.x` to stay on Spring Boot 2.x, or `-6.x`
if you are on Spring Boot 3.x/4.x. Getting the migration itself wrong is caught at build time — a
stale coordinate fails to resolve, and a stale import fails to compile.

**Upgrade the agent first, or together with the toolkit.** Compatibility is only backward, not
forward:

| Toolkit | Agent | Result |
|---|---|---|
| old `apm-toolkit-webflux` (<= 9.7.0) | 9.8.0+ | works — the agent still matches the un-versioned classes |
| `-5.x` / `-6.x` (9.8.0+) | <= 9.7.0 | **silently untraced** — the older agent does not know the `.v5` / `.v6` classes, so it never instruments them |

The second row compiles and runs perfectly normally; the only symptom is that the toolkit calls
stop producing spans. So do not roll out a 9.8.0+ toolkit against a 9.7.0 or older agent.

The following scenarios are supported for tracing assistance.

### Continue Tracing from Client
The `WebFluxSkyWalkingOperators#continueTracing` provides manual tracing continuous capabilities to adopt native Webflux APIs

With `apm-toolkit-webflux-5.x` (Reactor 3.1.3 - 3.4). `Mono#subscriberContext` was removed in
Reactor 3.5, so this form does not compile on Spring Boot 3.x/4.x:
```java
    @GetMapping("/testcase/annotation/mono/onnext") 
    public Mono<String> monoOnNext(@RequestBody(required = false) String body) {
        return Mono.subscriberContext()
            .flatMap(ctx -> WebFluxSkyWalkingOperators.continueTracing(ctx, () -> {
                visit("http://localhost:" + serverPort + "/testcase/success");
                return Mono.just("Hello World");
            }));
    }
```

The `ServerWebExchange` overload takes no Reactor context, so it is identical on both generations:
```java
    @GetMapping("/login/userFunctions")
    public Mono<Response<FunctionInfoResult>> functionInfo(ServerWebExchange exchange, @RequestParam String userId) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(context ->  {
                return exchange.getSession().map(session -> WebFluxSkyWalkingOperators.continueTracing(exchange, () -> handle(session, userId)));
            });
    }

    private Response<FunctionInfoResult> handle(WebSession session, String userId) {
        //...dubbo rpc    
    }
```

With `apm-toolkit-webflux-6.x` (Reactor 3.5+). `Mono#deferContextual` and `Context#of(ContextView)`
were added in Reactor 3.4, so this form requires Reactor 3.4 or later:
```java
    Mono.just("key").subscribeOn(Schedulers.boundedElastic())
        .doOnEach(WebFluxSkyWalkingOperators.continueTracing(SignalType.ON_NEXT, () -> log.info("test log with tid")))
        .flatMap(key -> Mono.deferContextual(ctx -> WebFluxSkyWalkingOperators.continueTracing(Context.of(ctx), () -> {
                redis.hasKey(key);
                return Mono.just("SUCCESS");
            })
        ));
...
```

### Fetch trace context relative IDs 
```java
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        // fetch trace ID
        String traceId = WebFluxSkyWalkingTraceContext.traceId(exchange);
        
        // fetch segment ID
        String segmentId = WebFluxSkyWalkingTraceContext.segmentId(exchange);
        
        // fetch span ID
        int spanId = WebFluxSkyWalkingTraceContext.spanId(exchange);
        
        return chain.filter(exchange);
    }
```

### Manipulate Correlation Context

```java
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        // Set correlation data can be retrieved by upstream nodes.
        WebFluxSkyWalkingTraceContext.putCorrelation(exchange, "key1", "value");
        
        // Get correlation data
        Optional<String> value2 = WebFluxSkyWalkingTraceContext.getCorrelation(exchange, "key2");
        
        // dosomething...
        
        return chain.filter(exchange);
    }
```

_Sample codes only_



