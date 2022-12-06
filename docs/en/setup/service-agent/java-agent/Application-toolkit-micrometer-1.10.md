# Observations

* Dependency the toolkit, such as using maven or gradle
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-micrometer-1.10</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```

* To use the Micrometer Observation Registry with Skywalking, you need to add handlers to the registry. Skywalking comes
with dedicated `SkywalkingMeterHandler` (for metrics) and `SkywalkingSenderTracingHandler`, `SkywalkingReceiverTracingHandler`
`SkywalkingDefaultTracingHandler` (for traces).

```java
// Here we create the Observation Registry with attached handlers
ObservationRegistry registry = ObservationRegistry.create();
// Here we add a meter handler
registry.observationConfig()
        .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
            new SkywalkingMeterHandler(new SkywalkingMeterRegistry())
);
// Here we add tracing handlers
registry.observationConfig()
        .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
            new SkywalkingSenderTracingHandler(), new SkywalkingReceiverTracingHandler(),
            new SkywalkingDefaultTracingHandler()
        ));
```

With such setup metrics and traces will be created for any Micrometer Observation based instrumentations.
