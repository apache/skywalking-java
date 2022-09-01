Changes by Version
==================
Release Notes.

8.12.0
------------------
* Fix `Shenyu plugin`'s NPE in reading trace ID when IgnoredTracerContext is used in the context.
* Update witness class in elasticsearch-6.x-plugin, avoid throw NPE.
* Fix `onHalfClose` using span operation name `/Request/onComplete` instead of the wrong name `/Request/onHalfClose`.
* Add plugin to support RESTeasy 4.x.
* Add plugin to support hutool-http 5.x.
* Add plugin to support Tomcat 10.x.
* Save http status code regardless of it's status.
* Upgrade byte-buddy to 1.12.13, and adopt byte-buddy APIs changes.
* Upgrade gson to 2.8.9.
* Upgrade netty-codec-http2 to 4.1.79.Final.
* Fix race condition causing agent to not reconnect after network error
* Force the injected high-priority classes in order to avoid NoClassDefFoundError.
* Plugin to support xxl-job 2.3.x.
* Add plugin to support Micronaut(HTTP Client/Server) 3.2.x-3.6.x
* Add plugin to support NATS Java client 2.14.x-2.15.x
* Remove inappropriate dependency from elasticsearch-7.x-plugin
* Upgrade jedis plugin to support 3.x(stream),4.x

#### Documentation

* Add a section in `Bootstrap-plugins` doc, introducing HttpURLConnection Plugin compatibility.
* Update `Plugin automatic test framework`, fix inconsistent description about configuration.yml.
* Update `Plugin automatic test framework`, add expected data format of the log items.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/138?closed=1)
