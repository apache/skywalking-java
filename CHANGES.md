Changes by Version
==================
Release Notes.

8.14.0
------------------

* Polish test framework to support `arm64/v8` platforms
* Fix wrong config name `plugin.toolkit.use_qualified_name_as_operation_name`, and system variable name `SW_PLUGIN_TOOLKIT_USE_QUALIFIED_NAME_AS_OPERATION_NAME:false`. They were **toolit**.
* Rename `JDBI` to `JDBC`
* Support collecting dubbo thread pool metrics
* Bump up byte-buddy to 1.12.19
* Upgrade agent test tools
* [Breaking Change] Compatible with 3.x and 4.x RabbitMQ Client, rename `rabbitmq-5.x-plugin` to `rabbitmq-plugin`
* Polish JDBC plugins to make DBType accurate
* Report the agent version to OAP as an instance attribute
* Polish jedis-4.x-plugin to change command to lowercase, which is consistent with jedis-2.x-3.x-plugin
* Add micronauthttpclient,micronauthttpserver,memcached,ehcache,guavacache,jedis,redisson plugin config properties to agent.config
* Add [Micrometer Observation](https://github.com/micrometer-metrics/micrometer/) support
* Add tags `mq.message.keys` and `mq.message.tags` for RocketMQ producer span
* Clean the trace context which injected into Pulsar MessageImpl after the instance recycled

#### Documentation

* Update `Plugin-test.md`, support string operators `start with` and `end with`
* Polish agent configurations doc to fix type error



All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/161?closed=1)

------------------
Find change logs of all versions [here](changes).
