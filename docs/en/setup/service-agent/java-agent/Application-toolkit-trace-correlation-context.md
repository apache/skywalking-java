* Use `TraceContext.putCorrelation()` API to put custom data in tracing context. 
```java
Optional<String> previous = TraceContext.putCorrelation("customKey", "customValue");
```
CorrelationContext will remove the item when the value is `null` or empty.

* Use `TraceContext.getCorrelation()` API to get custom data.
```java
Optional<String> value = TraceContext.getCorrelation("customKey");
```
CorrelationContext configuration descriptions could be found in [the agent configuration](README.md#table-of-agent-configuration-properties) documentation, with `correlation.` as the prefix.