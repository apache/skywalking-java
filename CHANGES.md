Changes by Version
==================
Release Notes.

9.8.0
------------------

* Add Spring LDAP 3.3.x-4.x plugin.
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

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/263?closed=1)

------------------
Find change logs of all versions [here](changes).
