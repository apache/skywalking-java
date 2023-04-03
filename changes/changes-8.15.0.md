Changes by Version
==================
Release Notes.

8.15.0
------------------

* Enhance lettuce plugin to adopt uniform tags.
* Expose complete Tracing APIs in the tracing toolkit.
* Add plugin to trace Spring 6 and Resttemplate 6.
* Move the baseline to JDK 17 for development, the runtime baseline is still Java 8 compatible.
* Remove Powermock entirely from the test cases.
* Fix H2 instrumentation point
* Refactor pipeline in jedis-plugin.
* Add plugin to support ClickHouse JDBC driver (0.3.2.*).
* Refactor kotlin coroutine plugin with CoroutineContext.
* Fix OracleURLParser ignoring actual port when :SID is absent.
* Change gRPC instrumentation point to fix plugin not working for server side.
* Fix servicecomb plugin trace break.
* Adapt Armeria's plugins to the latest version 1.22.x
* Fix tomcat-10x-plugin and add test case to support tomcat7.x-8.x-9.x.
* Fix thrift plugin generate duplicate traceid when `sendBase` error occurs
* Support keep trace profiling when cross-thread.
* Fix unexpected whitespace of the command catalogs in several Redis plugins.
* Fix a thread leak in `SamplingService` when updated sampling policy in the runtime.
* Support MySQL plugin tracing SQL parameters when useServerPrepStmts 
* Update the endpoint name of `Undertow` plugin to `Method:Path`.
* Build a dummy(empty) javadoc of finagle and jdk-http plugins due to incompatibility.

#### Documentation
* Update docs of Tracing APIs, reorganize the API docs into six parts.
* Correct missing package name in native manual API docs.
* Add a FAQ doc about "How to make SkyWalking agent works in `OSGI` environment?"

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/168?closed=1)
