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
* Support the agent version to report to OAP as an instance attribute

#### Documentation

* Update `Plugin-test.md`, support string operators `start with` and `end with`



All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/161?closed=1)

------------------
Find change logs of all versions [here](changes).
