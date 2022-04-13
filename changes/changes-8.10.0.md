Changes by Version
==================
Release Notes.

8.10.0
------------------

* [Important] Namespace represents a subnet, such as kubernetes namespace, or 172.10.*.*. Make namespace concept as a
  part of service naming format.
* [Important] Add cluster concept, also as a part of service naming format. The cluster name would be
    1. Add as {@link #SERVICE_NAME} suffix.
    2. Add as exit span's peer, ${CLUSTER} / original peer
    3. Cross Process Propagation Header's value addressUsedAtClient[index=8] (Target address of this request used on the
       client end).
* Support Undertow thread pool metrics collecting.
* Support Tomcat thread pool metric collect.
* Remove plugin for ServiceComb Java Chassis 0.x
* Add Guava EventBus plugin.
* Fix Dubbo 3.x plugin's tracing problem.
* Fix the bug that maybe generate multiple trace when invoke http request by spring webflux webclient.
* Support Druid Connection pool metrics collecting.
* Support HikariCP Connection pool metrics collecting.
* Support Dbcp2 Connection pool metrics collecting.
* Ignore the synthetic constructor created by the agent in the Spring patch plugin.
* Add witness class for vertx-core-3.x plugin.
* Add witness class for graphql plugin.
* Add vertx-core-4.x plugin.
* Renamed graphql-12.x-plugin to graphql-12.x-15.x-plugin and graphql-12.x-scenario to graphql-12.x-15.x-scenario.
* Add graphql-16plus plugin.
* [Test] Support to configure plugin test base images.
* [Breaking Change] Remove deprecated `agent.instance_properties` configuration.
  Recommend `agent.instance_properties_json`.
* The namespace and cluster would be reported as instance properties, keys are `namespace` and `cluster`. Notice, if
  instance_properties_json includes these two keys, they would be overrided by the agent core.
* [Breaking Change] Remove the namespace from `cross process propagation` key.
* Make sure the parent endpoint in tracing context from existing first ENTRY span, rather than first span only.
* Fix the bug that maybe causing memory leak and repeated traceId when use gateway-2.1.x-plugin or gateway-3.x-plugin.
* Fix Grpc 1.x plugin could leak context due to gRPC cancelled.
* Add JDK ThreadPoolExecutor Plugin.
* Support default database(not set through JDBC URL) in mysql-5.x plugin.

#### Documentation

* Add link about java agent injector.
* Update configurations doc, remove `agent.instance_properties[key]=value`.
* Update configurations doc, add `agent.cluster` and update `agent.namespace`.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/120?closed=1)
