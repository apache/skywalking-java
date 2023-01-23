* Use `TraceContext.traceId()` API to obtain traceId.
```java
import TraceContext;
...

modelAndView.addObject("traceId", TraceContext.traceId());
```
* Use `TraceContext.segmentId()` API to obtain segmentId.
```java
import TraceContext;
...

modelAndView.addObject("segmentId", TraceContext.segmentId());
```

* Use `TraceContext.spanId()` API to obtain spanId.
```java
import TraceContext;
...

modelAndView.addObject("spanId", TraceContext.spanId());
```