# logback plugin
* Dependency the toolkit, such as using maven or gradle
```xml
    <dependency>
         <groupId>org.apache.skywalking</groupId>
         <artifactId>apm-toolkit-logback-1.x</artifactId>
         <version>{project.release.version}</version>
     </dependency>
```

# Supported logback versions

The toolkit layouts support logback 1.2.x to 1.6.x in the same artifact, except logback **1.5.13**.
Using the layouts with logback 1.6.x requires toolkit 9.8.0 or later, as earlier toolkit releases fail on it with
`NoSuchFieldError: defaultConverterMap`. With an earlier toolkit release, declare the conversion words as
`<conversionRule>`s and use them in plain encoders instead, see [Print trace ID in your logs](#print-trace-id-in-your-logs).

Logback 1.5.13 has an upstream regression, [qos-ch/logback#885](https://github.com/qos-ch/logback/issues/885).
The layouts register `%tid`, `%sw_ctx`, `%X` and `%mdc` in the logging context's conversion rule registry
(`CoreConstants.PATTERN_RULE_REGISTRY`), which holds converter class names in every other release from 1.2 to 1.6.
Logback 1.5.13 changed the registry to hold converter suppliers, so class names that code puts into it break,
including this toolkit's and Spring Boot's. On 1.5.13, a layout whose pattern uses one of these words fails to start
with `ClassCastException: class java.lang.String cannot be cast to class java.util.function.Supplier`, and a
logback.xml containing it fails to configure. Logback 1.5.14 reverted the change, so upgrade to 1.5.14 or later.
`<conversionRule>`s declared in logback.xml are not affected.

# Print trace ID in your logs

* set `%tid` in `Pattern` section of logback.xml
```xml
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">
                <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%tid] [%thread] %-5level %logger{36} -%msg%n</Pattern>
            </layout>
        </encoder>
    </appender>
```

* with the MDC, set `%X{tid}` in `Pattern` section of logback.xml
```xml
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout">
                <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{tid}] [%thread] %-5level %logger{36} -%msg%n</Pattern>
            </layout>
        </encoder>
    </appender>
```

* to use `%tid` or `%sw_ctx` in other encoders, such as a plain `<encoder><pattern>`, declare them as conversion rules
  in logback.xml. Declare `X` with `org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.LogbackMDCPatternConverter`
  in the same way for `%X{tid}`. Logback 1.5.7+ also accepts `class` in place of the deprecated `converterClass`.
```xml
    <conversionRule conversionWord="tid"
                    converterClass="org.apache.skywalking.apm.toolkit.log.logback.v1.x.LogbackPatternConverter"/>
    <conversionRule conversionWord="sw_ctx"
                    converterClass="org.apache.skywalking.apm.toolkit.log.logback.v1.x.LogbackSkyWalkingContextPatternConverter"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%tid] [%thread] %-5level %logger{36} -%msg%n</pattern>
        </encoder>
    </appender>
```
  Before 9.8.0, the layouts registered these words for the whole JVM once their classes were loaded, so other encoders
  could pick them up by chance, depending on the configuration order. Since 9.8.0, the layouts register them in their
  logging context when they start, and the conversion rules are the way to use them anywhere else.


* Support logback AsyncAppender(MDC also support), No additional configuration is required. Refer to the demo of logback.xml below. For details: [Logback AsyncAppender](https://logback.qos.ch/manual/appenders.html#AsyncAppender)
```xml
    <configuration scan="true" scanPeriod=" 5 seconds">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout">
                    <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{tid}] [%thread] %-5level %logger{36} -%msg%n</Pattern>
                </layout>
            </encoder>
        </appender>
    
        <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
            <discardingThreshold>0</discardingThreshold>
            <queueSize>1024</queueSize>
            <neverBlock>true</neverBlock>
            <appender-ref ref="STDOUT"/>
        </appender>
    
        <root level="INFO">
            <appender-ref ref="ASYNC"/>
        </root>
    </configuration>
```

* When you use `-javaagent` to active the SkyWalking tracer, logback will output **traceId**, if it existed. If the tracer is inactive, the output will be `TID: N/A`.

# Print SkyWalking context in your logs

* Your only need to replace pattern `%tid` or `%X{tid]}` with `%sw_ctx` or `%X{sw_ctx}`.

* When you use `-javaagent` to active the SkyWalking tracer, logback will output `SW_CTX: [$serviceName,$instanceName,$traceId,$traceSegmentId,$spanId]`, if it existed. If the tracer is inactive, the output will be `SW_CTX: N/A`.

# logstash logback plugin

* Dependency the toolkit, such as using maven or gradle

```xml
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-logback-1.x</artifactId>
    <version>${skywalking.version}</version>
</dependency>
```

* set `LogstashEncoder` of logback.xml

```xml
<encoder charset="UTF-8" class="net.logstash.logback.encoder.LogstashEncoder">
    <!-- add TID(traceId) field -->
    <provider class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.logstash.TraceIdJsonProvider">
    </provider>
    <!-- add SW_CTX(SkyWalking context) field -->
    <provider class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.logstash.SkyWalkingContextJsonProvider">
    </provider>
</encoder>
```

* set `LoggingEventCompositeJsonEncoder` of logstash in logback-spring.xml for custom json format

1.add converter for %tid or %sw_ctx as child of <configuration> node
```xml
<!-- add converter for %tid -->
<conversionRule conversionWord="tid" converterClass="org.apache.skywalking.apm.toolkit.log.logback.v1.x.LogbackPatternConverter"/>
<!-- add converter for %sw_ctx -->    
<conversionRule conversionWord="sw_ctx" converterClass="org.apache.skywalking.apm.toolkit.log.logback.v1.x.LogbackSkyWalkingContextPatternConverter"/>
```
2.add json encoder for custom json format

```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <pattern>
                    <pattern>
                        {
                            "level": "%level",
                            "tid": "%tid",
                            "skyWalkingContext": "%sw_ctx",
                            "thread": "%thread",
                            "class": "%logger{1.}:%L",
                            "message": "%message",
                            "stackTrace": "%exception{10}"
                        }
                    </pattern>
                </pattern>
            </providers>
</encoder>
```

# gRPC reporter

The gRPC reporter could forward the collected logs to SkyWalking OAP server, or [SkyWalking Satellite sidecar](https://github.com/apache/skywalking-satellite). Trace id, segment id, and span id will attach to logs automatically. There is no need to modify existing layouts.

* Add `GRPCLogClientAppender` in logback.xml

```xml
    <appender name="grpc-log" class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.log.GRPCLogClientAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout">
                <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{tid}] [%thread] %-5level %logger{36} -%msg%n</Pattern>
            </layout>
        </encoder>
    </appender>
```

*  Add config of the plugin or use default

```properties
log.max_message_size=${SW_GRPC_LOG_MAX_MESSAGE_SIZE:10485760}
```

## Transmitting un-formatted messages

The logback 1.x gRPC reporter supports transmitting logs as formatted or un-formatted. Transmitting formatted data is the default but can be disabled by adding the following to the agent config:

```
plugin.toolkit.log.transmit_formatted=false
```

The above will result in the `content` field being used for the log pattern with additional log tags of `argument.0`, `argument.1`, and so on representing each logged argument as well as an additional `exception` tag which is only present if a throwable is also logged.

For example, the following code:
```java
log.info("{} {} {}", 1, 2, 3);
```

Will result in:
```json
{
  "content": "{} {} {}",
  "tags": [
    {
      "key": "argument.0",
      "value": "1"
    },
    {
      "key": "argument.1",
      "value": "2"
    },
    {
      "key": "argument.2",
      "value": "3"
    }
  ]
}
```
