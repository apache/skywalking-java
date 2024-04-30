# Advanced Reporters
The advanced report provides an alternative way to submit the agent collected data to the backend. All of them are in the `optional-reporter-plugins` folder, move the one you needed into the `plugins` folder for the activation. **Notice, don't try to activate multiple reporters, that could cause unexpected fatal errors.**

## Kafka Reporter
The Kafka reporter plugin support report traces, JVM metrics, Instance Properties, and profiled snapshots to Kafka cluster, which is disabled in default. Move the jar of the plugin, `kafka-reporter-plugin-x.y.z.jar`, from `agent/optional-reporter-plugins` to `agent/plugins` for activating.

If you configure to use `compression.type` such as `lz4`, `zstd`, `snappy`, etc., you also need to move the jar of the plugin, `lz4-java-x.y.z.jar` or `zstd-jni-x.y.z.jar` or `snappy-java.x.y.z.jar`, from `agent/optional-reporter-plugins` to `agent/plugins`.

Notice, currently, the agent still needs to configure GRPC receiver for delivering the task of profiling. In other words, the following configure cannot be omitted.

```properties
# Backend service addresses.
collector.backend_service=${SW_AGENT_COLLECTOR_BACKEND_SERVICES:127.0.0.1:11800}

# Kafka producer configuration
plugin.kafka.bootstrap_servers=${SW_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
plugin.kafka.get_topic_timeout=${SW_GET_TOPIC_TIMEOUT:10}
```

Before you activated the Kafka reporter, you have to make sure that `Kafka fetcher` of OAP server has been opened in service.

### Advanced Kafka Producer Configurations

Kafka reporter plugin support to customize all configurations of listed in [here](http://kafka.apache.org/24/documentation.html#producerconfigs). For example:
```properties
plugin.kafka.producer_config[delivery.timeout.ms]=12000
```

Since SkyWalking 8.8.0, support to configure advanced Producer configurations in JSON format, like this:
```properties
plugin.kafka.producer_config_json={"delivery.timeout.ms": 12000, "compression.type": "snappy"}
```

Currently, there are 2 ways to configure advanced configurations below. Notice that, the new way, configured in JSON format, will be overridden by `plugin.kafka.producer_config[key]=value` when they have the duplication keys.

Since 8.16.0, users could implement their decoder for kafka configurations rather than using plain configurations(such as `password`) of Kafka producer,
Including `plugin.kafka.producer_config_json`,`plugin.kafka.producer_config` or environment variable `SW_PLUGIN_KAFKA_PRODUCER_CONFIG_JSON`.

By doing that, add the `kafka-config-extension` dependency to your decoder project and implement `decode` interface.

- Add the `KafkaConfigExtension` dependency to your project.
```
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>kafka-config-extension</artifactId>
    <version>${skywalking.version}</version>
    <scope>provided</scope>
</dependency>
```

- Implement your custom decode method.Like this:
```
package org.apache.skywalking.apm.agent.sample;

import org.apache.skywalking.apm.agent.core.kafka.KafkaConfigExtension;
import java.util.Map;

/**
 * Custom decode class
 */
public class DecodeUtil implements KafkaConfigExtension {
    /**
     * Custom decode method.
     * @param config the value of `plugin.kafka.producer_config` or `plugin.kafka.producer_config_json` in `agent.config`.
     * @return the decoded configuration if you implement your custom decode logic.
     */
    public Map<String, String> decode(Map<String, String> config) {
        /**
         * implement your custom decode logic
         * */
        return config;
    }
}
```

Then, package your decoder project as a jar and move to `agent/plugins`.

**Notice, the jar package should contain all the dependencies required for your custom decode code.**

The last step is to activate the decoder class in `agent.config` like this:
```
plugin.kafka.decrypt_class="org.apache.skywalking.apm.agent.sample.DecodeUtil"
```
or configure by environment variable
```
SW_KAFKA_DECRYPT_CLASS="org.apache.skywalking.apm.agent.sample.DecodeUtil"
```

## 3rd party reporters
There are other reporter implementations from out of the Apache Software Foundation.

### Pulsar Reporter
Go to [Pulsar-reporter-plugin](https://github.com/SkyAPM/transporter-plugin-for-skywalking/blob/main/docs/en/pulsar/Pulsar-Reporter.md) for more details.

### RocketMQ Reporter
Go to [RocketMQ-reporter-plugin](https://github.com/SkyAPM/transporter-plugin-for-skywalking/blob/main/docs/en/rocketmq/Rocketmq-Reporter.md) for more details.
