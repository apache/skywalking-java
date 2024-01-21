Changes by Version
==================
Release Notes.

9.2.0
------------------

* Fix NoSuchMethodError in mvc-annotation-commons and change deprecated method.
* fix forkjoinpool plugin in JDK11。
* Support for tracing spring-cloud-gateway 4.x in gateway-4.x-plugin.
* Fix re-transform bug when plugin enhanced class proxy parent method.
* Fix error HTTP status codes not recording as SLA failures in Vert.x plugins. 
* Support for HttpExchange request tracing.
* Support tracing for async producing, batch sync consuming, and batch async consuming in rocketMQ-client-java-5.x-plugin.
* Convert the Redisson span into an async span.

#### Documentation


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/204?closed=1)

------------------
Find change logs of all versions [here](changes).
