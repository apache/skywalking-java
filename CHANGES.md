Changes by Version
==================
Release Notes.

8.12.0
------------------
* Fix `Shenyu plugin`'s NPE in reading read trace ID when IgnoredTracerContext is used in the context.
* Update witness class in elasticsearch-6.x-plugin, avoid throw NPE.
* Fix `onHalfClose` using span operation name `/Request/onComplete` instead of the worng name `/Request/onHalfClose`.
* Add plugin to support RESTeasy 4.x.
* Add plugin to support hutool-http 5.x.
* Save http status code regardless of it's status.

#### Documentation


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/138?closed=1)

------------------
Find change logs of all versions [here](changes).
