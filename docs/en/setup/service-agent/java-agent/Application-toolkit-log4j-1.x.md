* Dependency the toolkit, such as using maven or gradle
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-log4j-1.x</artifactId>
      <version>{project.release.version}</version>
   </dependency>
```

# Print trace ID in your logs

* Config a layout
```properties
log4j.appender.CONSOLE.layout=org.apache.skywalking.apm.toolkit.log.log4j.v1.x.TraceIdPatternLayout
```

* set `%T` in `layout.ConversionPattern` ( In 2.0-2016, you should use %x, [Why change?](https://github.com/wu-sheng/sky-walking/issues/77) )
```properties
log4j.appender.CONSOLE.layout.ConversionPattern=%d [%T] %-5p %c{1}:%L - %m%n
```

* When you use `-javaagent` to active the SkyWalking tracer, log4j will output **traceId**, if it existed. If the tracer is inactive, the output will be `TID: N/A`.

# Print SkyWalking context in your logs

* Your only need to replace pattern `%T` with `%T{SW_CTX}`.

* When you use `-javaagent` to active the SkyWalking tracer, log4j will output `SW_CTX: [$serviceName,$instanceName,$traceId,$traceSegmentId,$spanId]`, if it existed. If the tracer is inactive, the output will be `SW_CTX: N/A`.

# gRPC reporter

The gRPC report could forward the collected logs to SkyWalking OAP server, or [SkyWalking Satellite sidecar](https://github.com/apache/skywalking-satellite). Trace id, segment id, and span id will attach to logs automatically. You don't need to change the layout.

* Add `GRPCLogClientAppender` in log4j.properties

```properties
log4j.rootLogger=INFO,CustomAppender
log4j.appender.CustomAppender=org.apache.skywalking.apm.toolkit.log.log4j.v1.x.log.GRPCLogClientAppender
log4j.appender.CustomAppender.layout=org.apache.log4j.PatternLayout
log4j.appender.CustomAppender.layout.ConversionPattern=[%t] %-5p %c %x - %m%n
```

*  Add config of the plugin or use default

```properties
log.max_message_size=${SW_GRPC_LOG_MAX_MESSAGE_SIZE:10485760}
```
