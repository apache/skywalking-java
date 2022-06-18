Changes by Version
==================
Release Notes.

8.11.0
------------------

* Fix `cluster` and `namespace` value duplicated(`namespace` value) in properties report.
* Add layer field to event when reporting.
* Remove redundant `shade.package` property.
* Add servicecomb-2.x plugin and Testcase.
* Fix NPE in gateway plugin when the timer triggers webflux webclient call.
* Add an optional plugin, trace-sampler-cpu-policy-plugin, which could disable trace collecting in high CPU load.
* Change the dateformat of logs to `yyyy-MM-dd HH:mm:ss.SSS`(was `yyyy-MM-dd HH:mm:ss:SSS`).
* Fix NPE in elasticsearch plugin.
* Grpc plugin support trace client async generic call(without grpc stubs), support Method type: `UNARY`、`SERVER_STREAMING`.
* Enhance Apache ShenYu (incubating) plugin: support trace `grpc`,`sofarpc`,`motan`,`tars` rpc proxy.
* Add primary endpoint name to log events.
* Fix Span not finished in gateway plugin when the gateway request timeout.
* Support `-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector` in gRPC log report.
* Fix tcnative libraries relocation for aarch64.
* Add `plugin.jdbc.trace_sql_parameters` into Configuration Discovery Service.
* Fix argument type name of Array in postgresql-8.x-plugin from java.lang.String[] to [Ljava.lang.String; 
* Add type name checking in ArgumentTypeNameMatch and ReturnTypeNameMatch
* Highlight ArgumentTypeNameMatch and ReturnTypeNameMatch type naming rule in docs/en/setup/service-agent/java-agent/Java-Plugin-Development-Guide.md
* Fix FileWriter scheduled task NPE
* Optimize gRPC Log reporter to set service name for the first element in the streaming.(No change for Kafka reporter)


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/130?closed=1)
