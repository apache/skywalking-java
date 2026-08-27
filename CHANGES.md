Changes by Version
==================
Release Notes.

9.8.0
------------------

* Add Spring LDAP 3.3.x-4.x plugin.
* Fix the Log4j2 plugin descriptor (`Log4j2Plugins.dat`) missing from the `apm-toolkit-log4j-2.x` jar since 9.5.0, which broke `%traceId` and `%sw_ctx` resolution in Log4j2 `PatternLayout` (apache/skywalking#14006).

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/263?closed=1)

------------------
Find change logs of all versions [here](changes).
