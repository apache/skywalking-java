* Dependency the toolkit, such as using maven or gradle
# OpenTracing (Deprecated)

OpenTracing is a vendor-neutral standard for distributed tracing. It is a set of APIs that can be used to instrument, generate, collect, and report telemetry data for distributed systems. It is designed to be extensible so that new implementations can be created for new platforms or languages.
It had been archived by the CNCF TOC. [Learn more](https://www.cncf.io/blog/2022/01/31/cncf-archives-the-opentracing-project/).

SkyWalking community keeps the API compatible with 0.30.0 only. All further development will not be accepted. 

```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-opentracing</artifactId>
      <version>{project.release.version}</version>
   </dependency>
```


* Use our OpenTracing tracer implementation
```java
Tracer tracer = new SkywalkingTracer();
Tracer.SpanBuilder spanBuilder = tracer.buildSpan("/yourApplication/yourService");

```
