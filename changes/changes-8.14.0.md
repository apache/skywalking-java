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
* Fix In the higher version of mysql-connector-java 8x, there is an error in the value of db.instance.
* Add support for KafkaClients 3.x.
* Support to customize the collect period of JVM relative metrics.
* Upgrade netty-codec-http2 to 4.1.86.Final.
* Put `Agent-Version` property reading in the premain stage to avoid deadlock when using `jarsigner`.
* Add a config `agent.enable`(default: true) to support disabling the agent through system property `-Dskywalking.agent.disable=false` 
  or system environment variable setting `SW_AGENT_ENABLE=false`. 
* Enhance redisson plugin to adopt uniform tags.

#### Documentation

* Update `Plugin-test.md`, support string operators `start with` and `end with`
* Polish agent configurations doc to fix type error



All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/161?closed=1)
