Changes by Version
==================
Release Notes.

9.4.0
------------------

* Upgrade nats plugin to support 2.16.5
* Add agent self-observability.
* Fix intermittent ClassCircularityError by preloading ThreadLocalRandom since ByteBuddy 1.12.11
* Add witness class/method for resteasy-server plugin(v3/v4/v6)
* Add async-profiler feature for performance analysis 
* Support db.instance tag,db.collection tag and AggregateOperation span for mongodb plugin(3.x/4.x)
* Improve CustomizeConfiguration by avoiding repeatedly resolve file config
* Add empty judgment for constructorInterceptPoint
* Bump up gRPC to 1.68.1
* Bump up netty to 4.1.115.Final
* Fix the `CreateAopProxyInterceptor` in the Spring core-patch to prevent it from changing the implementation of the Spring AOP proxy  
* Support Tracing for GlobalFilter and GatewayFilter in Spring Gateway

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/222?closed=1)

------------------
Find change logs of all versions [here](changes).
