Changes by Version
==================
Release Notes.

9.8.0
------------------

* Fix the `httpclient-5.x-plugin` async client tracing (apache/skywalking#14097):
  * `FutureCallback` no longer stops a span. With `HttpAsyncClients.classic(...)` the callback runs on the caller
    thread, where it used to close the caller's own active span.
  * The HTTP exit span is now created in the caller's context when the request is sent, detached right away, and
    finished by reference when the response ends. Nothing is left on the I/O reactor thread's span stack any more,
    so overlapping requests on one reactor thread no longer nest into or close each other's spans.
  * The `httpasyncclient/local` span and its cross-thread segment are removed. The HTTP exit span now lives in the
    caller's own segment.
  * HttpClient 5.4+ async requests are traced now. Previously the plugin produced no spans for them, because
    `doExecute` received a `null` `HttpContext` and the request was not yet in the context when `IOSessionImpl#poll`
    ran.
  * The `httpclient-5.x-scenario` now tests one version per minor, 5.0 to 5.6.
* Fix the `NullPointerException` thrown by the `spring-webflux-5.x-webclient` and
  `spring-webflux-6.x-webclient` plugins when `DefaultClientRequestBuilder$BodyInserterRequest#writeTo` runs
  before any exit span exists. Connectors such as `JdkClientHttpConnector` call `writeTo` eagerly at assembly
  time, while the exchange interceptor creates the exit span and its `ContextCarrier` only at subscription, so
  the interception failed and the `sw8` header was not propagated. The carrier injection is now null-guarded
  and, if the carrier is still absent, retried when the returned `Mono` is subscribed (apache/skywalking#13589).
* Fix the `spring-ai-1.x-plugin` `ChatModelStreamInterceptor` leaking its async span when
  `ChatModel#stream(Prompt)` fails synchronously, which silently dropped the whole `TraceSegment`
  of the request (apache/skywalking#14098).
* Fix `jedis-4.x-plugin`'s `AbstractConnectionInterceptor` double-stopping the span stack on any
  Redis-level exception (or a null dynamic field on a pooled/recycled `Connection`), which corrupted
  the parent trace for the rest of the request (apache/skywalking#14085).
* Add Spring LDAP 3.3.x-4.x plugin.
* Exclude macOS metadata files from source and binary release archives (apache/skywalking#14080).
* Fix `NoSuchMethodError: org.apache.skywalking.apm.plugin.spring.webflux.v6.DispatcherHandlerHandleMethodInterceptor`
  on Spring Framework 7 (Spring Boot 4). `HttpHeaders` no longer implements `MultiValueMap`, so
  `List get(Object)` was removed; the entry span was never created and the service produced no
  traces. Affects `spring-webflux-6.x` and, through its shaded copy, `spring-cloud-gateway-4.x`
  (apache/skywalking#14047).
* Extend `spring-webflux-6.x`, `spring-webflux-6.x-webclient`, `springmvc-annotation-6.x`,
  `spring-resttemplate-6.x` and `spring-cloud-gateway-4.x` plugins to support Spring Framework 7,
  Spring Boot 4 and Spring Cloud Gateway 5.x.
* Add Spring version witnesses to the `spring-webflux-5.x` and `spring-webflux-6.x` plugins, so they
  no longer both match `DispatcherHandler#handle` on Reactor 3.4 (Spring Boot 2.4-2.7).
* **Breaking:** rename `apm-toolkit-webflux` to `apm-toolkit-webflux-5.x` and move its package to
  `org.apache.skywalking.apm.toolkit.webflux.v5`; add `apm-toolkit-webflux-6.x`
  (`org.apache.skywalking.apm.toolkit.webflux.v6`) for Reactor 3.5+ / Spring Boot 3.x and 4.x, where
  `WebFluxSkyWalkingOperators#continueTracing` previously threw `NoSuchMethodError` because Reactor
  removed `Signal#getContext()` in 3.5.0. Existing `apm-toolkit-webflux` jars (9.7.0 and earlier)
  remain instrumented by the agent, so upgrading the agent alone does not force a change.
* Fix the Log4j2 plugin descriptor (`Log4j2Plugins.dat`) missing from the `apm-toolkit-log4j-2.x` jar since 9.5.0, which broke `%traceId` and `%sw_ctx` resolution in Log4j2 `PatternLayout` (apache/skywalking#14006).
* Deploy the root `java-agent` POM to Maven Central again. It is the parent of
  `apm-application-toolkit` and therefore of every published toolkit artifact, but 9.7.0 skipped it,
  so resolving any `org.apache.skywalking:apm-toolkit-*:9.7.0` failed with `Non-resolvable parent POM
  ... Could not find artifact org.apache.skywalking:java-agent:pom:9.7.0` (apache/skywalking#13988).
* Stop reporting datasource timeout configuration as metrics. The c3p0 `maxIdleTime`, DBCP `maxWaitMillis`
  and HikariCP `connectionTimeout`, `validationTimeout`, `idleTimeout` and `leakDetectionThreshold` gauges are no longer reported.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/263?closed=1)

------------------
Find change logs of all versions [here](changes).
