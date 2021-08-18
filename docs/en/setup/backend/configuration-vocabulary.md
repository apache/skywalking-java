# Configuration Vocabulary
The Configuration Vocabulary lists all available configurations provided by `application.yml`.

Module | Provider | Settings | Value(s) and Explanation | System Environment Variable¹ | Default |
----------- | ---------- | --------- | --------- |--------- |--------- |
core|default|role|Option values: `Mixed/Receiver/Aggregator`. **Receiver** mode OAP opens the service to the agents, then analyzes and aggregates the results, and forwards the results for distributed aggregation. Aggregator mode OAP receives data from Mixer and Receiver role OAP nodes, and performs 2nd level aggregation. **Mixer** means both Receiver and Aggregator. |SW_CORE_ROLE|Mixed|
| - | - | restHost| Binding IP of RESTful services. Services include GraphQL query and HTTP data report. |SW_CORE_REST_HOST|0.0.0.0|
| - | - | restPort | Binding port of RESTful services. | SW_CORE_REST_PORT|12800|
| - | - | restContextPath| Web context path of RESTful services. | SW_CORE_REST_CONTEXT_PATH|/|
| - | - | restMinThreads| Minimum thread number of RESTful services. | SW_CORE_REST_JETTY_MIN_THREADS|1|
| - | - | restMaxThreads| Maximum thread number of RESTful services. | SW_CORE_REST_JETTY_MAX_THREADS|200|
| - | - | restIdleTimeOut| Connector idle timeout of RESTful services (in milliseconds). | SW_CORE_REST_JETTY_IDLE_TIMEOUT|30000|
| - | - | restAcceptorPriorityDelta| Thread priority delta to give to acceptor threads of RESTful services. | SW_CORE_REST_JETTY_DELTA|0|
| - | - | restAcceptQueueSize| ServerSocketChannel Backlog of RESTful services. | SW_CORE_REST_JETTY_QUEUE_SIZE|0|
| - | - | httpMaxRequestHeaderSize| Maximum request header size accepted. | SW_CORE_HTTP_MAX_REQUEST_HEADER_SIZE|8192|
| - | - | gRPCHost| Binding IP of gRPC services, including gRPC data report and internal communication among OAP nodes. |SW_CORE_GRPC_HOST|0.0.0.0|
| - | - | gRPCPort| Binding port of gRPC services. | SW_CORE_GRPC_PORT|11800|
| - | - | gRPCSslEnabled| Activates SSL for gRPC services. | SW_CORE_GRPC_SSL_ENABLED|false|
| - | - | gRPCSslKeyPath| File path of gRPC SSL key. | SW_CORE_GRPC_SSL_KEY_PATH| - |
| - | - | gRPCSslCertChainPath| File path of gRPC SSL cert chain. | SW_CORE_GRPC_SSL_CERT_CHAIN_PATH| - |
| - | - | gRPCSslTrustedCAPath| File path of gRPC trusted CA. | SW_CORE_GRPC_SSL_TRUSTED_CA_PATH| - |
| - | - | downsampling| Activated level of down sampling aggregation. | | Hour,Day|
| - | - | persistentPeriod| Execution period of the persistent timer (in seconds). | | 25 |
| - | - | enableDataKeeperExecutor| Controller of TTL scheduler. Once disabled, TTL wouldn't work. |SW_CORE_ENABLE_DATA_KEEPER_EXECUTOR|true|
| - | - | dataKeeperExecutePeriod| Execution period of TTL scheduler (in minutes). Execution doesn't mean deleting data. The storage provider (e.g. ElasticSearch storage) could override this.|SW_CORE_DATA_KEEPER_EXECUTE_PERIOD|5|
| - | - | recordDataTTL| The lifecycle of record data (in days). Record data includes traces, top N sample records, and logs. Minimum value is 2. |SW_CORE_RECORD_DATA_TTL|3|
| - | - | metricsDataTTL| The lifecycle of metrics data (in days), including metadata. We recommend setting metricsDataTTL >= recordDataTTL. Minimum value is 2. | SW_CORE_METRICS_DATA_TTL|7|
| - | - | l1FlushPeriod| The period of L1 aggregation flush to L2 aggregation (in milliseconds). | SW_CORE_L1_AGGREGATION_FLUSH_PERIOD | 500 |
| - | - | storageSessionTimeout| The threshold of session time (in milliseconds). Default value is 70000. | SW_CORE_STORAGE_SESSION_TIMEOUT | 70000 |
| - | - | enableDatabaseSession| Cache metrics data for 1 minute to reduce database queries, and if the OAP cluster changes within that minute. |SW_CORE_ENABLE_DATABASE_SESSION|true|
| - | - | topNReportPeriod|The execution period (in minutes) of top N sampler, which saves sampled data into the storage. |SW_CORE_TOPN_REPORT_PERIOD|10|
| - | - | activeExtraModelColumns|Appends entity names (e.g. service names) into metrics storage entities. |SW_CORE_ACTIVE_EXTRA_MODEL_COLUMNS|false|
| - | - | serviceNameMaxLength| Maximum length limit of service names. |SW_SERVICE_NAME_MAX_LENGTH|70|
| - | - | instanceNameMaxLength| Maximum length limit of service instance names. The maximum length of service + instance names should be less than 200.|SW_INSTANCE_NAME_MAX_LENGTH|70|
| - | - | endpointNameMaxLength| Maximum length limit of endpoint names. The maximum length of service + endpoint names should be less than 240.|SW_ENDPOINT_NAME_MAX_LENGTH|150|
| - | - | searchableTracesTags | Defines a set of span tag keys which are searchable through GraphQL. Multiple values are separated by commas. | SW_SEARCHABLE_TAG_KEYS | http.method,status_code,db.type,db.instance,mq.queue,mq.topic,mq.broker|
| - | - | searchableLogsTags | Defines a set of log tag keys which are searchable through GraphQL. Multiple values are separated by commas. | SW_SEARCHABLE_LOGS_TAG_KEYS | level |
| - | - | searchableAlarmTags | Defines a set of alarm tag keys which are searchable through GraphQL. Multiple values are separated by commas. | SW_SEARCHABLE_ALARM_TAG_KEYS | level |
| - | - | gRPCThreadPoolSize| Pool size of gRPC server. | SW_CORE_GRPC_THREAD_POOL_SIZE | CPU core * 4|
| - | - | gRPCThreadPoolQueueSize| Queue size of gRPC server. | SW_CORE_GRPC_POOL_QUEUE_SIZE | 10000|
| - | - | maxConcurrentCallsPerConnection | The maximum number of concurrent calls permitted for each incoming connection. Defaults to no limit. | SW_CORE_GRPC_MAX_CONCURRENT_CALL | - |
| - | - | maxMessageSize | Sets the maximum message size allowed to be received on the server. Empty means 4 MiB. | SW_CORE_GRPC_MAX_MESSAGE_SIZE | 4M(based on Netty) |
| - | - | remoteTimeout | Timeout for cluster internal communication (in seconds). | - |20|
| - | - | maxSizeOfNetworkAddressAlias| The maximum size of network address detected in the system being monitored. | - | 1_000_000|
| - | - | maxPageSizeOfQueryProfileSnapshot| The maximum size for snapshot analysis in an OAP query. | - | 500 |
| - | - | maxSizeOfAnalyzeProfileSnapshot| The maximum number of snapshots analyzed by the OAP. | - | 12000 |
| - | - | prepareThreads| The number of threads used to prepare metrics data to the storage. | SW_CORE_PREPARE_THREADS | 2 |
| - | - | enableEndpointNameGroupingByOpenapi | Automatically groups endpoints by the given OpenAPI definitions. | SW_CORE_ENABLE_ENDPOINT_NAME_GROUPING_BY_OPAENAPI | true |
|cluster|standalone| - | Standalone is not suitable for running on a single node running. No configuration available. | - | - |
| - | zookeeper|nameSpace| The namespace, represented by root path, isolates the configurations in Zookeeper.|SW_NAMESPACE| `/`, root path|
| - | - | hostPort| Hosts and ports of Zookeeper Cluster. |SW_CLUSTER_ZK_HOST_PORT| localhost:2181|
| - | - | baseSleepTimeMs| The period of Zookeeper client between two retries (in milliseconds). |SW_CLUSTER_ZK_SLEEP_TIME|1000|
| - | - | maxRetries| The maximum retry time. |SW_CLUSTER_ZK_MAX_RETRIES|3|
| - | - | enableACL| Opens ACL using `schema` and `expression`. |SW_ZK_ENABLE_ACL| false|
| - | - | schema | Schema for the authorization. |SW_ZK_SCHEMA|digest|
| - | - | expression | Expression for the authorization. |SW_ZK_EXPRESSION|skywalking:skywalking|
| - | - | internalComHost| The hostname registered in Zookeeper for the internal communication of OAP cluster. | - | -|
| - | - | internalComPort| The port registered in Zookeeper for the internal communication of OAP cluster. | - | -1|
| - | kubernetes| namespace| Namespace deployed by SkyWalking in k8s. |SW_CLUSTER_K8S_NAMESPACE|default|
| - | - | labelSelector| Labels used for filtering OAP deployment in k8s. |SW_CLUSTER_K8S_LABEL| app=collector,release=skywalking|
| - | - | uidEnvName| Environment variable name for reading uid. | SW_CLUSTER_K8S_UID|SKYWALKING_COLLECTOR_UID|
| - | consul| serviceName| Service name for SkyWalking cluster. |SW_SERVICE_NAME|SkyWalking_OAP_Cluster|
| - | - | hostPort| Hosts and ports for Consul cluster.| SW_CLUSTER_CONSUL_HOST_PORT|localhost:8500|
| - | - | aclToken| ACL Token of Consul. Empty string means `without ALC token`. | SW_CLUSTER_CONSUL_ACLTOKEN | - |
| - | - | internalComHost| The hostname registered in Consul for internal communications of the OAP cluster. | - | -|
| - | - | internalComPort| The port registered in Consul for internal communications of the OAP cluster. | - | -1|
| - | etcd| serviceName| Service name for SkyWalking cluster. |SW_CLUSTER_ETCD_SERVICE_NAME|SkyWalking_OAP_Cluster|
| - | - | endpoints| Hosts and ports for etcd cluster. | SW_CLUSTER_ETCD_ENDPOINTS|localhost:2379|
| - | - | namespace | Namespace for SkyWalking cluster. |SW_CLUSTER_ETCD_NAMESPACE | /skywalking |
| - | - | authentication | Indicates whether there is authentication. | SW_CLUSTER_ETCD_AUTHENTICATION | false |
| - | - | user | Etcd auth username. | SW_CLUSTER_ETCD_USER | |
| - | - | password | Etcd auth password. | SW_CLUSTER_ETCD_PASSWORD | |
| - | Nacos| serviceName| Service name for SkyWalking cluster. |SW_SERVICE_NAME|SkyWalking_OAP_Cluster|
| - | - | hostPort| Hosts and ports for Nacos cluster.| SW_CLUSTER_NACOS_HOST_PORT|localhost:8848|
| - | - | namespace| Namespace used by SkyWalking node coordination. | SW_CLUSTER_NACOS_NAMESPACE|public|
| - | - | internalComHost| The hostname registered in Nacos for internal communications of the OAP cluster. | - | -|
| - | - | internalComPort| The port registered in Nacos for internal communications of the OAP cluster. | - | -1|
| - | - | username | Nacos Auth username. | SW_CLUSTER_NACOS_USERNAME | - |
| - | - | password | Nacos Auth password. | SW_CLUSTER_NACOS_PASSWORD | - |
| - | - | accessKey | Nacos Auth accessKey. | SW_CLUSTER_NACOS_ACCESSKEY | - |
| - | - | secretKey | Nacos Auth secretKey.  | SW_CLUSTER_NACOS_SECRETKEY | - |
| storage|elasticsearch| - | ElasticSearch 6 storage implementation. | - | - |
| - | - | nameSpace | Prefix of indexes created and used by SkyWalking. | SW_NAMESPACE | - |
| - | - | clusterNodes | ElasticSearch cluster nodes for client connection.| SW_STORAGE_ES_CLUSTER_NODES |localhost|
| - | - | protocol | HTTP or HTTPs. | SW_STORAGE_ES_HTTP_PROTOCOL | HTTP|
| - | - | connectTimeout | Connect timeout of ElasticSearch client (in milliseconds). | SW_STORAGE_ES_CONNECT_TIMEOUT | 500|
| - | - | socketTimeout | Socket timeout of ElasticSearch client (in milliseconds). | SW_STORAGE_ES_SOCKET_TIMEOUT | 30000|
| - | - | user| Username of ElasticSearch cluster. | SW_ES_USER | - |
| - | - | password | Password of ElasticSearch cluster. | SW_ES_PASSWORD | - |
| - | - | trustStorePath | Trust JKS file path. Only works when username and password are enabled. | SW_STORAGE_ES_SSL_JKS_PATH | - |
| - | - | trustStorePass | Trust JKS file password. Only works when username and password are enabled. | SW_STORAGE_ES_SSL_JKS_PASS | - |
| - | - | secretsManagementFile| Secrets management file in the properties format, including username and password, which are managed by a 3rd party tool. Capable of being updated them at runtime. |SW_ES_SECRETS_MANAGEMENT_FILE | - |
| - | - | dayStep| Represents the number of days in the one-minute/hour/day index. | SW_STORAGE_DAY_STEP | 1|
| - | - | indexShardsNumber | Shard number of new indexes. | SW_STORAGE_ES_INDEX_SHARDS_NUMBER | 1 |
| - | - | indexReplicasNumber | Replicas number of new indexes. | SW_STORAGE_ES_INDEX_REPLICAS_NUMBER | 0 |
| - | - | superDatasetDayStep | Represents the number of days in the super size dataset record index. Default value is the same as dayStep when the value is less than 0. |SW_SUPERDATASET_STORAGE_DAY_STEP|-1 |
| - | - | superDatasetIndexShardsFactor | Super dataset is defined in the code (e.g. trace segments). This factor provides more shards for the super dataset: shards number = indexShardsNumber * superDatasetIndexShardsFactor. This factor also affects Zipkin and Jaeger traces. |SW_STORAGE_ES_SUPER_DATASET_INDEX_SHARDS_FACTOR|5 |
| - | - | superDatasetIndexReplicasNumber | Represents the replicas number in the super size dataset record index. |SW_STORAGE_ES_SUPER_DATASET_INDEX_REPLICAS_NUMBER|0 |
| - | - | indexTemplateOrder| The order of index template. | SW_STORAGE_ES_INDEX_TEMPLATE_ORDER| 0|
| - | - | bulkActions| Async bulk size of the record data batch execution. | SW_STORAGE_ES_BULK_ACTIONS| 5000|
| - | - | flushInterval| Period of flush (in seconds). Does not matter whether `bulkActions` is reached or not. INT(flushInterval * 2/3) is used for index refresh period. | SW_STORAGE_ES_FLUSH_INTERVAL | 15 (index refresh period = 10)|
| - | - | concurrentRequests| The number of concurrent requests allowed to be executed. | SW_STORAGE_ES_CONCURRENT_REQUESTS| 2 |
| - | - | resultWindowMaxSize | The maximum size of dataset when the OAP loads cache, such as network aliases. | SW_STORAGE_ES_QUERY_MAX_WINDOW_SIZE | 10000|
| - | - | metadataQueryMaxSize | The maximum size of metadata per query. | SW_STORAGE_ES_QUERY_MAX_SIZE | 5000 |
| - | - | segmentQueryMaxSize | The maximum size of trace segments per query. | SW_STORAGE_ES_QUERY_SEGMENT_SIZE | 200|
| - | - | profileTaskQueryMaxSize | The maximum size of profile task per query. | SW_STORAGE_ES_QUERY_PROFILE_TASK_SIZE | 200|
| - | - | advanced | All settings of ElasticSearch index creation. The value should be in JSON format. | SW_STORAGE_ES_ADVANCED | - |
| - |elasticsearch7| - | ElasticSearch 7 storage implementation. | - | - |
| - | - | nameSpace | Prefix of indexes created and used by SkyWalking. | SW_NAMESPACE | - |
| - | - | clusterNodes | ElasticSearch cluster nodes for client connection.| SW_STORAGE_ES_CLUSTER_NODES |localhost|
| - | - | protocol | HTTP or HTTPs. | SW_STORAGE_ES_HTTP_PROTOCOL | HTTP|
| - | - | connectTimeout | Connect timeout of ElasticSearch client (in milliseconds). | SW_STORAGE_ES_CONNECT_TIMEOUT | 500|
| - | - | socketTimeout | Socket timeout of ElasticSearch client (in milliseconds). | SW_STORAGE_ES_SOCKET_TIMEOUT | 30000|
| - | - | user| Username of ElasticSearch cluster.| SW_ES_USER | - |
| - | - | password | Password of ElasticSearch cluster. | SW_ES_PASSWORD | - |
| - | - | trustStorePath | Trust JKS file path. Only works when username and password are enabled. | SW_STORAGE_ES_SSL_JKS_PATH | - |
| - | - | trustStorePass | Trust JKS file password. Only works when username and password are enabled. | SW_STORAGE_ES_SSL_JKS_PASS | - |
| - | - | secretsManagementFile| Secrets management file in the properties format, including username and password, which are managed by a 3rd party tool. Capable of being updated at runtime. |SW_ES_SECRETS_MANAGEMENT_FILE | - |
| - | - | dayStep| Represents the number of days in the one-minute/hour/day index. | SW_STORAGE_DAY_STEP | 1|
| - | - | indexShardsNumber | Shard number of new indexes. | SW_STORAGE_ES_INDEX_SHARDS_NUMBER | 1 |
| - | - | indexReplicasNumber | Replicas number of new indexes. | SW_STORAGE_ES_INDEX_REPLICAS_NUMBER | 0 |
| - | - | superDatasetDayStep | Represents the number of days in the super size dataset record index. Default value is the same as dayStep when the value is less than 0. |SW_SUPERDATASET_STORAGE_DAY_STEP|-1 |
| - | - | superDatasetIndexShardsFactor | Super dataset is defined in the code (e.g. trace segments). This factor provides more shards for the super dataset: shards number = indexShardsNumber * superDatasetIndexShardsFactor. This factor also affects Zipkin and Jaeger traces. |SW_STORAGE_ES_SUPER_DATASET_INDEX_SHARDS_FACTOR|5 |
| - | - | superDatasetIndexReplicasNumber | Represents the replicas number in the super size dataset record index. |SW_STORAGE_ES_SUPER_DATASET_INDEX_REPLICAS_NUMBER|0 |
| - | - | indexTemplateOrder| The order of index template. | SW_STORAGE_ES_INDEX_TEMPLATE_ORDER| 0|
| - | - | bulkActions| Async bulk size of data batch execution. | SW_STORAGE_ES_BULK_ACTIONS| 5000|
| - | - | flushInterval| Period of flush (in seconds). Does not matter whether `bulkActions` is reached or not. INT(flushInterval * 2/3) is used for index refresh period. | SW_STORAGE_ES_FLUSH_INTERVAL | 15 (index refresh period = 10)|
| - | - | concurrentRequests| The number of concurrent requests allowed to be executed. | SW_STORAGE_ES_CONCURRENT_REQUESTS| 2 |
| - | - | resultWindowMaxSize | The maximum size of dataset when the OAP loads cache, such as network aliases. | SW_STORAGE_ES_QUERY_MAX_WINDOW_SIZE | 10000|
| - | - | metadataQueryMaxSize | The maximum size of metadata per query. | SW_STORAGE_ES_QUERY_MAX_SIZE | 5000 |
| - | - | segmentQueryMaxSize | The maximum size of trace segments per query. | SW_STORAGE_ES_QUERY_SEGMENT_SIZE | 200|
| - | - | profileTaskQueryMaxSize | The maximum size of profile task per query. | SW_STORAGE_ES_QUERY_PROFILE_TASK_SIZE | 200|
| - | - | advanced | All settings of ElasticSearch index creation. The value should be in JSON format. | SW_STORAGE_ES_ADVANCED | - |
| - |h2| - |  H2 storage is designed for demonstration and running in short term (i.e. 1-2 hours) only. | - | - |
| - | - | driver | H2 JDBC driver. | SW_STORAGE_H2_DRIVER | org.h2.jdbcx.JdbcDataSource|
| - | - | url | H2 connection URL. Defaults to H2 memory mode. | SW_STORAGE_H2_URL | jdbc:h2:mem:skywalking-oap-db |
| - | - | user | Username of H2 database. | SW_STORAGE_H2_USER | sa |
| - | - | password | Password of H2 database. | - | - | 
| - | - | metadataQueryMaxSize | The maximum size of metadata per query. | SW_STORAGE_H2_QUERY_MAX_SIZE | 5000 |
| - | - | maxSizeOfArrayColumn | Some entities (e.g. trace segments) include the logic column with multiple values. In H2, we use multiple physical columns to host the values: e.g. change column_a with values [1,2,3,4,5] to `column_a_0 = 1, column_a_1 = 2, column_a_2 = 3 , column_a_3 = 4, column_a_4 = 5`. | SW_STORAGE_MAX_SIZE_OF_ARRAY_COLUMN | 20 |
| - | - | numOfSearchableValuesPerTag | In a trace segment, this includes multiple spans with multiple tags. Different spans may have the same tag key, e.g. multiple HTTP exit spans all have their own `http.method` tags. This configuration sets the limit on the maximum number of values for the same tag key. | SW_STORAGE_NUM_OF_SEARCHABLE_VALUES_PER_TAG | 2 |
| - |mysql| - | MySQL Storage. The MySQL JDBC Driver is not in the dist. Please copy it into the oap-lib folder manually. | - | - |
| - | - | properties | Hikari connection pool configurations. | - | Listed in the `application.yaml`. |
| - | - | metadataQueryMaxSize | The maximum size of metadata per query. | SW_STORAGE_MYSQL_QUERY_MAX_SIZE | 5000 |
| - | - | maxSizeOfArrayColumn | Some entities (e.g. trace segments) include the logic column with multiple values. In MySQL, we use multiple physical columns to host the values, e.g. change column_a with values [1,2,3,4,5] to `column_a_0 = 1, column_a_1 = 2, column_a_2 = 3 , column_a_3 = 4, column_a_4 = 5`. | SW_STORAGE_MAX_SIZE_OF_ARRAY_COLUMN | 20 |
| - | - | numOfSearchableValuesPerTag | In a trace segment, this includes multiple spans with multiple tags. Different spans may have same tag key, e.g. multiple HTTP exit spans all have their own `http.method` tags. This configuration sets the limit on the maximum number of values for the same tag key. | SW_STORAGE_NUM_OF_SEARCHABLE_VALUES_PER_TAG | 2 |
| - |postgresql| - | PostgreSQL storage. | - | - |
| - | - | properties | Hikari connection pool configurations. | - | Listed in the `application.yaml`. |
| - | - | metadataQueryMaxSize | The maximum size of metadata per query. | SW_STORAGE_MYSQL_QUERY_MAX_SIZE | 5000 |
| - | - | maxSizeOfArrayColumn | Some entities (e.g. trace segments) include the logic column with multiple values. In PostgreSQL, we use multiple physical columns to host the values, e.g. change column_a with values [1,2,3,4,5] to `column_a_0 = 1, column_a_1 = 2, column_a_2 = 3 , column_a_3 = 4, column_a_4 = 5` | SW_STORAGE_MAX_SIZE_OF_ARRAY_COLUMN | 20 |
| - | - | numOfSearchableValuesPerTag | In a trace segment, this includes multiple spans with multiple tags. Different spans may have same tag key, e.g. multiple HTTP exit spans all have their own `http.method` tags. This configuration sets the limit on the maximum number of values for the same tag key. | SW_STORAGE_NUM_OF_SEARCHABLE_VALUES_PER_TAG | 2 |
| - |influxdb| - | InfluxDB storage. |- | - |
| - | - | url| InfluxDB connection URL. | SW_STORAGE_INFLUXDB_URL | http://localhost:8086|
| - | - | user | User name of InfluxDB. | SW_STORAGE_INFLUXDB_USER | root|
| - | - | password | Password of InfluxDB. | SW_STORAGE_INFLUXDB_PASSWORD | -|
| - | - | database | Database of InfluxDB. | SW_STORAGE_INFLUXDB_DATABASE | skywalking |
| - | - | actions | The number of actions to collect. | SW_STORAGE_INFLUXDB_ACTIONS | 1000 |
| - | - | duration | The maximum waiting time (in milliseconds). | SW_STORAGE_INFLUXDB_DURATION | 1000|
| - | - | batchEnabled | If true, write points with batch API. | SW_STORAGE_INFLUXDB_BATCH_ENABLED | true|
| - | - | fetchTaskLogMaxSize | The maximum number of fetch task log in a request. | SW_STORAGE_INFLUXDB_FETCH_TASK_LOG_MAX_SIZE | 5000|
| - | - | connectionResponseFormat | The response format of connection to influxDB. It can only be MSGPACK or JSON. | SW_STORAGE_INFLUXDB_CONNECTION_RESPONSE_FORMAT | MSGPACK |
| agent-analyzer | default | Agent Analyzer. | SW_AGENT_ANALYZER | default |
| - | -| sampleRate| Sampling rate for receiving trace. Precise to 1/10000. 10000 means a sampling rate of 100% by default.|SW_TRACE_SAMPLE_RATE|10000|
| - | - |slowDBAccessThreshold| The slow database access threshold (in milliseconds). |SW_SLOW_DB_THRESHOLD|default:200,mongodb:100|
| - | - |forceSampleErrorSegment| When sampling mechanism is activated, this config samples the error status segment and ignores the sampling rate. |SW_FORCE_SAMPLE_ERROR_SEGMENT|true|
| - | - |segmentStatusAnalysisStrategy| Determines the final segment status from span status. Available values are `FROM_SPAN_STATUS` , `FROM_ENTRY_SPAN`, and `FROM_FIRST_SPAN`. `FROM_SPAN_STATUS` indicates that the segment status would be error if any span has an error status. `FROM_ENTRY_SPAN` means that the segment status would only be determined by the status of entry spans. `FROM_FIRST_SPAN` means that the segment status would only be determined by the status of the first span. |SW_SEGMENT_STATUS_ANALYSIS_STRATEGY|FROM_SPAN_STATUS|
| - | - |noUpstreamRealAddressAgents| Exit spans with the component in the list would not generate client-side instance relation metrics, since some tracing plugins (e.g. Nginx-LUA and Envoy) can't collect the real peer IP address. |SW_NO_UPSTREAM_REAL_ADDRESS|6000,9000|
| - | - |slowTraceSegmentThreshold| Setting this threshold on latency (in milliseconds) would cause the slow trace segments to be sampled if they use up more time, even if the sampling mechanism is activated. The default value is `-1`, which means that slow traces would not be sampled. |SW_SLOW_TRACE_SEGMENT_THRESHOLD|-1|
| - | - |meterAnalyzerActiveFiles| Indicates which files could be instrumented and analyzed. Multiple files are split by ",". |SW_METER_ANALYZER_ACTIVE_FILES||
| receiver-sharing-server|default| Sharing server provides new gRPC and restful servers for data collection. Ana designates that servers in the core module are to be used for internal communication only. | - | - |
| - | - | restHost| Binding IP of RESTful services. Services include GraphQL query and HTTP data report. | SW_RECEIVER_SHARING_REST_HOST | - |
| - | - | restPort | Binding port of RESTful services. | SW_RECEIVER_SHARING_REST_PORT | - |
| - | - | restContextPath| Web context path of RESTful services. | SW_RECEIVER_SHARING_REST_CONTEXT_PATH | - |
| - | - | restMinThreads| Minimum thread number of RESTful services. | SW_RECEIVER_SHARING_JETTY_MIN_THREADS|1|
| - | - | restMaxThreads| Maximum thread number of RESTful services. | SW_RECEIVER_SHARING_JETTY_MAX_THREADS|200|
| - | - | restIdleTimeOut| Connector idle timeout of RESTful services (in milliseconds). | SW_RECEIVER_SHARING_JETTY_IDLE_TIMEOUT|30000|
| - | - | restAcceptorPriorityDelta| Thread priority delta to give to acceptor threads of RESTful services. | SW_RECEIVER_SHARING_JETTY_DELTA|0|
| - | - | restAcceptQueueSize| ServerSocketChannel backlog of RESTful services. | SW_RECEIVER_SHARING_JETTY_QUEUE_SIZE|0|
| - | - | httpMaxRequestHeaderSize| Maximum request header size accepted. | SW_RECEIVER_SHARING_HTTP_MAX_REQUEST_HEADER_SIZE|8192|
| - | - | gRPCHost| Binding IP of gRPC services. Services include gRPC data report and internal communication among OAP nodes. | SW_RECEIVER_GRPC_HOST | 0.0.0.0. Not Activated |
| - | - | gRPCPort| Binding port of gRPC services. | SW_RECEIVER_GRPC_PORT | Not Activated |
| - | - | gRPCThreadPoolSize| Pool size of gRPC server. | SW_RECEIVER_GRPC_THREAD_POOL_SIZE | CPU core * 4|
| - | - | gRPCThreadPoolQueueSize| Queue size of gRPC server. | SW_RECEIVER_GRPC_POOL_QUEUE_SIZE | 10000|
| - | - | gRPCSslEnabled| Activates SSL for gRPC services. | SW_RECEIVER_GRPC_SSL_ENABLED | false |
| - | - | gRPCSslKeyPath| File path of gRPC SSL key. | SW_RECEIVER_GRPC_SSL_KEY_PATH | - |
| - | - | gRPCSslCertChainPath| File path of gRPC SSL cert chain. | SW_RECEIVER_GRPC_SSL_CERT_CHAIN_PATH | - |
| - | - | maxConcurrentCallsPerConnection | The maximum number of concurrent calls permitted for each incoming connection. Defaults to no limit. | SW_RECEIVER_GRPC_MAX_CONCURRENT_CALL | - |
| - | - | authentication | The token text for authentication. Works for gRPC connection only. Once this is set, the client is required to use the same token. | SW_AUTHENTICATION | - |
| log-analyzer | default | Log Analyzer. | SW_LOG_ANALYZER | default |
| - | - | lalFiles | The LAL configuration file names (without file extension) to be activated. Read [LAL](../../concepts-and-designs/lal.md) for more details. | SW_LOG_LAL_FILES | default |
| - | - | malFiles | The MAL configuration file names (without file extension) to be activated. Read [LAL](../../concepts-and-designs/lal.md) for more details. | SW_LOG_MAL_FILES | "" |
| event-analyzer | default | Event Analyzer. | SW_EVENT_ANALYZER | default |
| receiver-register|default| Read [receiver doc](backend-receivers.md) for more details. | - | - |
| receiver-trace|default| Read [receiver doc](backend-receivers.md) for more details. | - | - |
| receiver-jvm| default| Read [receiver doc](backend-receivers.md) for more details. | - | - |
| receiver-clr| default| Read [receiver doc](backend-receivers.md) for more details. | - | - |
| receiver-profile| default| Read [receiver doc](backend-receivers.md) for more details. | - | - |
| receiver-zabbix| default| Read [receiver doc](backend-zabbix.md) for more details. | - | - |
| - | - | port| Exported TCP port. Zabbix agent could connect and transport data. | SW_RECEIVER_ZABBIX_PORT | 10051 |
| - | - | host| Binds to host. | SW_RECEIVER_ZABBIX_HOST | 0.0.0.0 |
| - | - | activeFiles| Enables config when agent request is received. | SW_RECEIVER_ZABBIX_ACTIVE_FILES | agent |
| service-mesh| default| Read [receiver doc](backend-receivers.md) for more details. | - | - |
| envoy-metric| default| Read [receiver doc](backend-receivers.md) for more details. | - | - |
| - | - | acceptMetricsService | Starts Envoy Metrics Service analysis. | SW_ENVOY_METRIC_SERVICE | true|
| - | - | alsHTTPAnalysis | Starts Envoy HTTP Access Log Service analysis. Value = `k8s-mesh` means starting the analysis. | SW_ENVOY_METRIC_ALS_HTTP_ANALYSIS | - |
| - | - | alsTCPAnalysis | Starts Envoy TCP Access Log Service analysis. Value = `k8s-mesh` means starting the analysis. | SW_ENVOY_METRIC_ALS_TCP_ANALYSIS | - |
| - | - | k8sServiceNameRule | `k8sServiceNameRule` allows you to customize the service name in ALS via Kubernetes metadata. The available variables are `pod` and `service`. E.g. you can use `${service.metadata.name}-${pod.metadata.labels.version}` to append the version number to the service name. Note that when using environment variables to pass this configuration, use single quotes(`''`) to avoid being evaluated by the shell. | - |
| receiver-otel | default | Read [receiver doc](backend-receivers.md) for more details. | - | - |
| - | - | enabledHandlers| Enabled handlers for otel. | SW_OTEL_RECEIVER_ENABLED_HANDLERS | - |
| - | - | enabledOcRules| Enabled metric rules for OC handler. | SW_OTEL_RECEIVER_ENABLED_OC_RULES | - |
| receiver_zipkin |default| Read [receiver doc](backend-receivers.md). | - | - |
| - | - | restHost| Binding IP of RESTful services. |SW_RECEIVER_ZIPKIN_HOST|0.0.0.0|
| - | - | restPort | Binding port of RESTful services. | SW_RECEIVER_ZIPKIN_PORT|9411|
| - | - | restContextPath| Web context path of RESTful services. | SW_RECEIVER_ZIPKIN_CONTEXT_PATH|/|
| receiver_jaeger | default| Read [receiver doc](backend-receivers.md). | - | - |
| - | - | gRPCHost|Binding IP of gRPC services. Services include gRPC data report and internal communication among OAP nodes. | SW_RECEIVER_JAEGER_HOST | - |
| - | - | gRPCPort| Binding port of gRPC services. | SW_RECEIVER_JAEGER_PORT | - |
| - | - | gRPCThreadPoolSize| Pool size of gRPC server. | - | CPU core * 4|
| - | - | gRPCThreadPoolQueueSize| Queue size of gRPC server. | - | 10000|
| - | - | maxConcurrentCallsPerConnection | The maximum number of concurrent calls permitted for each incoming connection. Defaults to no limit. | - | - |
| - | - | maxMessageSize | Sets the maximum message size allowed to be received on the server. Empty means 4 MiB. | - | 4M(based on Netty) |
| prometheus-fetcher | default | Read [fetcher doc](backend-fetcher.md) for more details. | - | - |
| - | - | enabledRules | Enabled rules. | SW_PROMETHEUS_FETCHER_ENABLED_RULES | self |
| - | - | maxConvertWorker | The maximize meter convert worker. | SW_PROMETHEUS_FETCHER_NUM_CONVERT_WORKER | -1(by default, half the number of CPU core(s)) |   
| kafka-fetcher | default | Read [fetcher doc](backend-fetcher.md) for more details. | - | - |
| - | - | bootstrapServers | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster. | SW_KAFKA_FETCHER_SERVERS | localhost:9092 |
| - | - | namespace | Namespace aims to isolate multi OAP cluster when using the same Kafka cluster. If you set a namespace for Kafka fetcher, OAP will add a prefix to topic name. You should also set namespace in `agent.config`. The property is named `plugin.kafka.namespace`. | SW_NAMESPACE | - |
| - | - | groupId | A unique string that identifies the consumer group to which this consumer belongs.| - | skywalking-consumer |
| - | - | consumePartitions | Indicates which PartitionId(s) of the topics is/are assigned to the OAP server. Separated by commas if multiple. | SW_KAFKA_FETCHER_CONSUME_PARTITIONS | - |
| - | - | isSharding | True when OAP Server is in cluster. | SW_KAFKA_FETCHER_IS_SHARDING | false |
| - | - | createTopicIfNotExist | If true, this creates Kafka topic (if it does not already exist). | - | true |
| - | - | partitions | The number of partitions for the topic being created. | SW_KAFKA_FETCHER_PARTITIONS | 3 |
| - | - | enableNativeProtoLog | Enables fetching and handling native proto log data. | SW_KAFKA_FETCHER_ENABLE_NATIVE_PROTO_LOG | false |
| - | - | enableNativeJsonLog | Enables fetching and handling native json log data. | SW_KAFKA_FETCHER_ENABLE_NATIVE_JSON_LOG | false |
| - | - | replicationFactor | The replication factor for each partition in the topic being created. | SW_KAFKA_FETCHER_PARTITIONS_FACTOR | 2 |
| - | - | kafkaHandlerThreadPoolSize | Pool size of Kafka message handler executor. | SW_KAFKA_HANDLER_THREAD_POOL_SIZE | CPU core * 2 |
| - | - | kafkaHandlerThreadPoolQueueSize | Queue size of Kafka message handler executor. | SW_KAFKA_HANDLER_THREAD_POOL_QUEUE_SIZE | 10000 |
| - | - | topicNameOfMeters | Kafka topic name for meter system data. | - | skywalking-meters |
| - | - | topicNameOfMetrics | Kafka topic name for JVM metrics data. | - | skywalking-metrics |
| - | - | topicNameOfProfiling | Kafka topic name for profiling data. | - | skywalking-profilings |
| - | - | topicNameOfTracingSegments | Kafka topic name for tracing data. | - | skywalking-segments |
| - | - | topicNameOfManagements | Kafka topic name for service instance reporting and registration. | - | skywalking-managements |
| - | - | topicNameOfLogs | Kafka topic name for native proto log data. | - | skywalking-logs |
| - | - | topicNameOfJsonLogs | Kafka topic name for native json log data. | - | skywalking-logs-json |
| receiver-browser | default | Read [receiver doc](backend-receivers.md) for more details. | - | - | - |
| - | - | sampleRate | Sampling rate for receiving trace. Precise to 1/10000. 10000 means sampling rate of 100% by default. | SW_RECEIVER_BROWSER_SAMPLE_RATE | 10000 |
| query | graphql | - | GraphQL query implementation. | - |
| - | - | path | Root path of GraphQL query and mutation. | SW_QUERY_GRAPHQL_PATH | /graphql|
| - | - | enableLogTestTool | Enable the log testing API to test the LAL. **NOTE**: This API evaluates untrusted code on the OAP server. A malicious script can do significant damage (steal keys and secrets, remove files and directories, install malware, etc). As such, please enable this API only when you completely trust your users. | SW_QUERY_GRAPHQL_ENABLE_LOG_TEST_TOOL | false |
| alarm | default | - | Read [alarm doc](backend-alarm.md) for more details. | - |
| telemetry | - | - | Read [telemetry doc](backend-telemetry.md) for more details. | - |
| - | none| - | No op implementation. | - |
| - | prometheus| host | Binding host for Prometheus server fetching data. | SW_TELEMETRY_PROMETHEUS_HOST|0.0.0.0|
| - | - | port|  Binding port for Prometheus server fetching data. |SW_TELEMETRY_PROMETHEUS_PORT|1234|
| configuration | - | - | Read [dynamic configuration doc](dynamic-config.md) for more details. | - |
| - | grpc| host | DCS server binding hostname. | SW_DCS_SERVER_HOST | - |
| - | - | port | DCS server binding port. | SW_DCS_SERVER_PORT | 80 |
| - | - | clusterName | Cluster name when reading the latest configuration from DSC server. | SW_DCS_CLUSTER_NAME | SkyWalking|
| - | - | period | The period of reading data from DSC server by the OAP (in seconds). | SW_DCS_PERIOD | 20 |
| - | apollo| apolloMeta| `apollo.meta` in Apollo. | SW_CONFIG_APOLLO | http://106.12.25.204:8080 | 
| - | - | apolloCluster | `apollo.cluster` in Apollo. | SW_CONFIG_APOLLO_CLUSTER | default|
| - | - | apolloEnv | `env` in Apollo. | SW_CONFIG_APOLLO_ENV | - |
| - | - | appId | `app.id` in Apollo. | SW_CONFIG_APOLLO_APP_ID | skywalking |
| - | - | period | The period of data sync (in seconds). | SW_CONFIG_APOLLO_PERIOD | 60 |
| - | zookeeper|nameSpace| The namespace (represented by root path) that isolates the configurations in the Zookeeper. |SW_CONFIG_ZK_NAMESPACE| `/`, root path|
| - | - | hostPort| Hosts and ports of Zookeeper Cluster. |SW_CONFIG_ZK_HOST_PORT| localhost:2181|
| - | - | baseSleepTimeMs|The period of Zookeeper client between two retries (in milliseconds). |SW_CONFIG_ZK_BASE_SLEEP_TIME_MS|1000|
| - | - | maxRetries| The maximum retry time. |SW_CONFIG_ZK_MAX_RETRIES|3|
| - | - | period | The period of data sync (in seconds). | SW_CONFIG_ZK_PERIOD | 60 |
| - | etcd| endpoints | Hosts and ports for etcd cluster (separated by commas if multiple). | SW_CONFIG_ETCD_ENDPOINTS | localhost:2379 | 
| - | - | namespace | Namespace for SkyWalking cluster. |SW_CONFIG_ETCD_NAMESPACE | /skywalking |
| - | - | authentication | Indicates whether there is authentication. | SW_CONFIG_ETCD_AUTHENTICATION | false |
| - | - | user | Etcd auth username. | SW_CONFIG_ETCD_USER | |
| - | - | password | Etcd auth password. | SW_CONFIG_ETCD_PASSWORD | |
| - | - | period | The period of data sync (in seconds). | SW_CONFIG_ZK_PERIOD | 60
| - | consul | hostPort| Hosts and ports for Consul cluster.| SW_CONFIG_CONSUL_HOST_AND_PORTS|localhost:8500|
| - | - | aclToken| ACL Token of Consul. Empty string means `without ACL token`.| SW_CONFIG_CONSUL_ACL_TOKEN | - |
| - | - | period | The period of data sync (in seconds). | SW_CONFIG_CONSUL_PERIOD | 60 |
| - | k8s-configmap | namespace | Deployment namespace of the config map. |SW_CLUSTER_K8S_NAMESPACE|default|
| - | - | labelSelector| Labels for locating configmap. |SW_CLUSTER_K8S_LABEL|app=collector,release=skywalking|
| - | - | period | The period of data sync (in seconds). | SW_CONFIG_ZK_PERIOD | 60 |
| - | nacos | serverAddr | Nacos Server Host. | SW_CONFIG_NACOS_SERVER_ADDR | 127.0.0.1|
| - | - | port | Nacos Server Port. | SW_CONFIG_NACOS_SERVER_PORT | 8848 |
| - | - | group | Nacos Configuration namespace. | SW_CONFIG_NACOS_SERVER_NAMESPACE | - |
| - | - | period | The period of data sync (in seconds). | SW_CONFIG_CONFIG_NACOS_PERIOD | 60 |
| - | - | username | Nacos Auth username. | SW_CONFIG_NACOS_USERNAME | - |
| - | - | password | Nacos Auth password. | SW_CONFIG_NACOS_PASSWORD | - |
| - | - | accessKey | Nacos Auth accessKey. | SW_CONFIG_NACOS_ACCESSKEY | - |
| - | - | secretKey | Nacos Auth secretKey.  | SW_CONFIG_NACOS_SECRETKEY | - |
| exporter | grpc | targetHost | The host of target gRPC server for receiving export data. | SW_EXPORTER_GRPC_HOST | 127.0.0.1 |
| - | - | targetPort | The port of target gRPC server for receiving export data. | SW_EXPORTER_GRPC_PORT | 9870 |
| health-checker | default | checkIntervalSeconds | The period of checking OAP internal health status (in seconds). | SW_HEALTH_CHECKER_INTERVAL_SECONDS | 5 |
| configuration-discovery | default | disableMessageDigest | If true, agent receives the latest configuration every time, even without making any changes. By default, OAP uses the SHA512 message digest mechanism to detect changes in configuration. | SW_DISABLE_MESSAGE_DIGEST | false
| receiver-event|default| Read [receiver doc](backend-receivers.md) for more details. | - | - |

## Note
¹ System Environment Variable name could be declared and changed in `application.yml`. The names listed here are simply provided in the default `application.yml` file.
