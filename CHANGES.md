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

#### Documentation


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/130?closed=1)

------------------
Find change logs of all versions [here](changes).
