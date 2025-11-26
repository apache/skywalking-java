Changes by Version
==================
Release Notes.

9.6.0
------------------

* Bump up agent-oap protocol to latest(16c51358ebcf42629bf4ffdf952253971f20eb25).
* Bump up gRPC to v1.74.0.
* Bump up netty to v4.1.124.Final.
* Bump up GSON to v2.13.1.
* Bump up guava to v32.1.3.
* Bump up oap to the 10.3-dev.latest(dc8740d4757b35374283c4850a9a080e40f0eb79) in e2e.
* Bump up cli to the 0.15.0-dev.latest(77b4c49e89c9c000278f44e62729d534f2ec842e) in e2e.
* Bump up apache parent pom to v35.
* Update Maven to 3.6.3 in mvnw.
* Fix OOM due to too many span logs.
* Fix ClassLoader cache OOM issue with WeakHashMap.
* Fix Jetty client cannot receive the HTTP response body.
* Eliminate repeated code with HttpServletRequestWrapper in mvc-annotation-commons.
* Add the jdk httpclient plugin.
* Fix Gateway 2.0.x plugin not activated for spring-cloud-starter-gateway 2.0.0.RELEASE.
* Support kafka-clients-3.9.x intercept.
* Upgrade kafka-clients version in optional-reporter-plugins to 3.9.1.
* Fix AbstractLogger replaceParam when the replaced string contains a replacement marker.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/242?closed=1)

------------------
Find change logs of all versions [here](changes).
