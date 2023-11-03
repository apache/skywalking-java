Changes by Version
==================
Release Notes.

9.1.0
------------------

* Fix hbase onConstruct NPE in the file configuration scenario
* Fix the issue of createSpan failure caused by invalid request URL in HttpClient 4.x/5.x plugin
* Optimize ElasticSearch 6.x 7.x plugin compatibility
* Fix an issue with the httpasyncclient component where the isError state is incorrect.
* Support customization for the length limitation of string configurations
* Add max length configurations in `agent.config` file for service_name and instance_name
* Optimize spring-cloud-gateway 2.1.x, 3.x witness class.
* Support report MongoDB instance info in Mongodb 4.x plugin.
* To compatible upper and lower case Oracle TNS url parse.
* Fix config length limitation.
* Support collecting ZGC memory pool metrics. Require OAP 9.7.0 to support these new metrics.
* Upgrade netty-codec-http2 to 4.1.100.Final
* Add a netty-http 4.1.x plugin to trace HTTP requests.
* Fix Impala Jdbc URL (including schema without properties) parsing exception.
* Optimize byte-buddy type description performance.

#### Documentation

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/194?closed=1)

------------------
Find change logs of all versions [here](changes).
