Changes by Version
==================
Release Notes.

9.2.0
------------------

* Fix NoSuchMethodError in mvc-annotation-commons and change deprecated method.
* Fix forkjoinpool plugin in JDK11.
* Support for tracing spring-cloud-gateway 4.x in gateway-4.x-plugin.
* Fix re-transform bug when plugin enhanced class proxy parent method.
* Fix error HTTP status codes not recording as SLA failures in Vert.x plugins. 
* Support for HttpExchange request tracing.
* Support tracing for async producing, batch sync consuming, and batch async consuming in rocketMQ-client-java-5.x-plugin.
* Convert the Redisson span into an async span.
* Rename system env name from `sw_plugin_kafka_producer_config` to `SW_PLUGIN_KAFKA_PRODUCER_CONFIG`.
* Support for ActiveMQ-Artemis messaging tracing.
* Archive the expired plugins `impala-jdbc-2.6.x-plugin`.
* Fix a bug in Spring Cloud Gateway if HttpClientFinalizer#send does not invoke, the span created at NettyRoutingFilterInterceptor can not stop.
* Fix not tracing in HttpClient v5 when HttpHost(arg[0]) is null but `RoutingSupport#determineHost` works.
* Support across thread tracing for SOFA-RPC.
* Update Jedis 4.x plugin to support Sharding and Cluster models.

#### Documentation
* Update docs to describe `expired-plugins`.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/204?closed=1)
