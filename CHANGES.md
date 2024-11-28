Changes by Version
==================
Release Notes.

9.4.0
------------------

* Upgrade nats plugin to support 2.16.5
* Add agent self-observability.
* Fix intermittent ClassCircularityError by preloading ThreadLocalRandom since ByteBuddy 1.12.11
* Add witness class/method for resteasy-server plugin(v3/v4/v6)
* Add async-profiler feature for performance analysis 
* Support db.instance tag,db.collection tag and AggregateOperation span for mongodb plugin(3.x/4.x)
* Improve CustomizeConfiguration by avoiding repeatedly resolve file config

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/222?closed=1)

------------------
Find change logs of all versions [here](changes).
