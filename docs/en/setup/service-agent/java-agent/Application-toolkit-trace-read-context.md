* Use `TraceContext.traceId()` API to obtain traceId.
```java
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
...

modelAndView.addObject("traceId", TraceContext.traceId());
```
* Use `TraceContext.segmentId()` API to obtain segmentId.
```java
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
...

modelAndView.addObject("segmentId", TraceContext.segmentId());
```

* Use `TraceContext.spanId()` API to obtain spanId.
```java
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
...

modelAndView.addObject("spanId", TraceContext.spanId());
```
_Sample codes only_