# Mannual propagation of tracing context for Webflux
* Dependency the toolkit, such as using maven or gradle
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-webflux</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```

* usage 1.
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
* usage 2.
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

* usage 3.
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

* usage 4.
```java
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        // get traceId
        String traceId = WebFluxSkyWalkingTraceContext.traceId(exchange);
        
        // set correlation data, it can be retrieved by downstream nodes.
        WebFluxSkyWalkingTraceContext.putCorrelation(exchange, "key", "value");
        return chain.filter(exchange);
    }
```

_Sample codes only_



