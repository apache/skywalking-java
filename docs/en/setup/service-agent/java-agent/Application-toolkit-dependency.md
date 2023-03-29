Dependency the toolkit, such as using maven or gradle
# Add Trace Toolkit
[`apm-toolkit-trace`](https://mvnrepository.com/artifact/org.apache.skywalking/apm-toolkit-trace) provides the APIs to enhance the trace context, 
such as `createLocalSpan`, `createExitSpan`, `createEntrySpan`, `log`, `tag`, `prepareForAsync` and `asyncFinish`.
Add the toolkit dependency to your project.


```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-trace</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```