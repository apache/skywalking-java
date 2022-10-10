Changes by Version
==================
Release Notes.

8.13.0
------------------

* Support set-type in the agent or plugin configurations
* Optimize ConfigInitializer to output warning messages when the config value is truncated.
* Fix the default value of the Map field would merge rather than override by new values in the config.
* Support to set the value of Map/List field to an empty map/list.
* Add plugin to support [Impala JDBC](https://www.cloudera.com/downloads/connectors/impala/jdbc/2-6-29.html) 2.6.x.
* Update guava-cache, jedis, memcached, ehcache plugins to adopt uniform tags.
* Fix `Apache ShenYu` plugin traceId empty string value. 
* Add plugin to support [brpc-java-3.x](https://github.com/baidu/starlight/tree/brpc-java-v3)
* Redisson param beautify

#### Documentation

* Update `configuration` doc about overriding default value as empty map/list accordingly.
* Update plugin dev tags for cache relative tags.
* Add plugin dev docs for virtual database tags.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/150?closed=1)

------------------
Find change logs of all versions [here](changes).
