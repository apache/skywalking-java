Changes by Version
==================
Release Notes.

8.13.0
------------------

**This release begins to adopt SkyWalking 9.3.0+ [Virtual Cache Analysis](https://skywalking.apache.org/docs/main/next/en/setup/service-agent/virtual-cache/),[Virtual MQ Analysis](https://skywalking.apache.org/docs/main/next/en/setup/service-agent/virtual-mq/)**

* Support set-type in the agent or plugin configurations
* Optimize ConfigInitializer to output warning messages when the config value is truncated.
* Fix the default value of the Map field would merge rather than override by new values in the config.
* Support to set the value of Map/List field to an empty map/list.
* Add plugin to support [Impala JDBC](https://www.cloudera.com/downloads/connectors/impala/jdbc/2-6-29.html) 2.6.x.
* Update guava-cache, jedis, memcached, ehcache plugins to adopt uniform tags.
* Fix `Apache ShenYu` plugin traceId empty string value. 
* Add plugin to support [brpc-java-3.x](https://github.com/baidu/starlight/tree/brpc-java-v3)
* Update `compose-start-script.template` to make compatible with new version docker compose
* Bump up grpc to 1.50.0 to fix CVE-2022-3171
* Polish up nats plugin to unify MQ related tags  
* Correct the duration of the transaction span for Neo4J 4.x.
* Plugin-test configuration.yml dependencies support docker service command field
* Polish up rabbitmq-5.x plugin to fix missing broker tag on consumer side
* Polish up activemq plugin to fix missing broker tag on consumer side
* Enhance MQ plugin relative tests to check key tags not blank.
* Add RocketMQ test scenarios for version 4.3 - 4.9. No 4.0 - 4.2 release images for testing.
* Support mannual propagation of tracing context to next operators for webflux.
* Add MQ_TOPIC and MQ_BROKER tags for RocketMQ consumer's span. 
* Polish up Pulsar plugins to remove unnecessary dynamic value , set peer at consumer side 
* Polish Kafka plugin to set peer at the consumer side.
* Polish NATS plugin to set peer at the consumer side.
* Polish ActiveMQ plugin to set peer at the consumer side.
* Polish RabbitMQ plugin to set peer at the consumer side.

#### Documentation

* Update `configuration` doc about overriding default value as empty map/list accordingly.
* Update plugin dev tags for cache relative tags.
* Add plugin dev docs for virtual database tags.
* Add plugin dev docs for virtual MQ tags.
* Add doc about kafka plugin Manual APIs.


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/150?closed=1)
