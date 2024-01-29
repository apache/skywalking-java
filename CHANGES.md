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

#### Documentation


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/204?closed=1)

------------------
Find change logs of all versions [here](changes).
