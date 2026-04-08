Changes by Version
==================
Release Notes.

9.7.0
------------------

* Added support for Lettuce reactive Redis commands.
* Add Spring AI 1.x plugin and GenAI layer.
* Fix httpclient-5.x plugin injecting sw8 propagation headers into ClickHouse HTTP requests (port 8123), causing HTTP 400. Add `PROPAGATION_EXCLUDE_PORTS` config to skip tracing (including header injection) for specified ports in the classic client interceptor.
* Add Spring RabbitMQ 2.x - 4.x plugin.
* Extend MySQL plugin to support MySQL Connector/J 8.4.0 and 9.x (9.0 -> 9.6).
* Extend MariaDB plugin to support MariaDB Connector/J 2.7.x.
* Extend MongoDB 4.x plugin to support MongoDB Java Driver 4.2 -> 4.10. Fix db.bind_vars extraction for driver 4.9+ where InsertOperation/DeleteOperation/UpdateOperation classes were removed.
* Extend Feign plugin to support OpenFeign 10.x, 11.x, 12.1.
* Extend Undertow plugin to support Undertow 2.1.x, 2.2.x, 2.3.x.
* Extend GraphQL plugin to support graphql-java 18 -> 24 (20+ requires JDK 17).
* Extend Spring Kafka plugin to support Spring Kafka 3.0 -> 3.3.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/249?closed=1)

------------------
Find change logs of all versions [here](changes).
