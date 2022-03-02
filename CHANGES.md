
Changes by Version
==================
Release Notes.

8.10.0
------------------
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
* [Test] Add `tomcat:10.0-jdk8-openjdk` and `tomcat:10.1-jdk11-openjdk` as plugin test base images.

#### Documentation
* Add link about java agent injector.


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/120?closed=1)

------------------
Find change logs of all versions [here](changes).
