# Webflux Tracing Assistant APIs

These APIs provide advanced features to enhance interaction capabilities in Webflux cases.

Add the toolkit to your project dependency, through Maven or Gradle
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-webflux</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```

The following scenarios are supported for tracing assistance.

### Continue Tracing from Client
The `WebFluxSkyWalkingOperators#continueTracing` provides manual tracing continuous capabilities to adopt native Webflux APIs

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



