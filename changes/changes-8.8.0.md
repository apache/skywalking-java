Changes by Version
==================
Release Notes.

8.8.0
------------------

* **Split Java agent from the main monorepo. It is a separate repository and going to release separately**.
* Support JDK 8-17 through upgrading byte-buddy to 1.11.18.
* Upgrade JDK 11 in dockerfile and remove unused java_opts.
* DataCarrier changes a `#consume` API to add properties as a parameter to initialize consumer when
  use `Class<? extends IConsumer<T>> consumerClass`.
* Support Multiple DNS period resolving mechanism
* Modify `Tags.STATUS_CODE` field name to `Tags.HTTP_RESPONSE_STATUS_CODE` and type from `StringTag` to `IntegerTag`, add `Tags.RPC_RESPONSE_STATUS_CODE` field to hold rpc response code value.
* Fix kafka-reporter-plugin shade package conflict
* Add all config items to `agent.conf` file for convenient containerization use cases.
* Advanced Kafka Producer configuration enhancement.
* Support mTLS for gRPC channel.
* fix the bug that plugin record wrong time elapse for lettuce plugin
* fix the bug that the wrong db.instance value displayed on Skywalking-UI when existing multi-database-instance on same host port pair.
* Add thrift plugin support thrift TMultiplexedProcessor.
* Add benchmark result for `exception-ignore` plugin and polish plugin guide.
* Provide Alibaba Druid database connection pool plugin.
* Provide HikariCP database connection pool plugin.
* Fix NumberFormat exception in jdbc-commons plugin when MysqlURLParser parser jdbcurl
* Provide Alibaba Fastjson parser/generator plugin.
* Provide Jackson serialization and deserialization plugin.
* Fix a tracing context leak of SpringMVC plugin, when an internal exception throws due to response can't be found.
* Make GRPC log reporter sharing GRPC channel with other reporters of agent. Remove config items of `agent.conf`, `plugin.toolkit.log.grpc.reporter.server_host`, `plugin.toolkit.log.grpc.reporter.server_port`, and `plugin.toolkit.log.grpc.reporter.upstream_timeout`.
    rename `plugin.toolkit.log.grpc.reporter.max_message_size` to `log.max_message_size`.
* Implement Kafka Log Reporter. Add config item of `agnt.conf`, `plugin.kafka.topic_logging`.
* Add plugin to support Apache HttpClient 5.
* Format SpringMVC & Tomcat EntrySpan operation name to `METHOD:URI`.
* Make `HTTP method` in the operation name according to runtime, rather than previous code-level definition, which used to have possibilities including multiple HTTP methods.
* Fix the bug that httpasyncclient-4.x-plugin does not take effect every time.
* Add plugin to support ClickHouse JDBC driver.
* Fix version compatibility for JsonRPC4J plugin.
* Add plugin to support Apache Kylin-jdbc 2.6.x 3.x 4.x
* Fix instrumentation v2 API doesn't work for constructor instrumentation.
* Add plugin to support okhttp 2.x
* Optimize okhttp 3.x 4.x plugin to get span time cost precisely
* Adapt message header properties of RocketMQ 4.9.x
* Fix httpasyncclient-4.x-plugin's memory leak risk

#### Documentation

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/99?closed=1)

