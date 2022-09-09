Changes by Version
==================
Release Notes.

8.13.0
------------------

* Support set-type in the agent or plugin configurations
* Optimize ConfigInitializer to output warning messages when the config value is truncated.
* Fix the default value of the Map field would merge rather than override by new values in the config.
* Support to set the value of Map/List field to an empty map/list.
* Update guava-cache,jedis,memcached,ehcache plugins to adapt uniform cache tag

#### Documentation

* Update `configuration` doc about overriding default value as empty map/list accordingly.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/150?closed=1)

------------------
Find change logs of all versions [here](changes).
