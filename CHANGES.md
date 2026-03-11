Changes by Version
==================
Release Notes.

9.7.0
------------------

* Added support for Lettuce reactive Redis commands.
* Add Spring AI 1.x plugin and GenAI layer.
* Fix httpclient-5.x plugin injecting sw8 propagation headers into ClickHouse HTTP requests (port 8123), causing HTTP 400. Add `PROPAGATION_EXCLUDE_PORTS` config to skip header injection for specified ports.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/249?closed=1)

------------------
Find change logs of all versions [here](changes).
