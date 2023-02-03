# Create Span

* Use `Tracer.createEntrySpan()` API to create entry span, and then use `SpanRef` to contain the reference of created span in agent kernel. The first parameter is operation name of span and the second parameter is the `ContextCarrierRef` instance which is the reference of contextcarrier in agent kernel. If the second parameter is not null, the process of creating entry span will do the extract operation which will be introduced in **inject/extract** scenario.

  ```java
  import org.apache.skywalking.apm.toolkit.trace.Tracer;
  ...
    
  SpanRef spanRef = Tracer.createEnteySpan("${operationName}", null);
  ```

* Use `Tracer.createLocalSpan()` API to create local span, the only parameter is the operation name of span.

  ```java
  import org.apache.skywalking.apm.toolkit.trace.Tracer;
  ...
    
  SpanRef spanRef = Tracer.createLocalSpan("${operationName}");
  ```

* Use `Tracer.createExitSpan()` API to create exit span

  * **two parameters case**: the first parameter is the operation name of span, the second parameter is the remote peer which means the peer address of exit operation.

    ```java
    import org.apache.skywalking.apm.toolkit.trace.Tracer;
    ...
      
    SpanRef spanRef = Tracer.createExitSpan("${operationName}", "${remotePeer}");
    ```

  * **three parameters case**: the first parameter is the operation name of span, the second parameter is the `ContextCarrierRef` instance and the third parameter is the remote peer. This case will be introduced in **inject/extract** scenario.

* Use `Tracer.stopSpan()` API to stop current span

  ```java
  import org.apache.skywalking.apm.toolkit.trace.Tracer;
  ...
    
  Tracer.stopSpan();
  ```

# Inject/Extract Context Carrier

The Inject/extract is to pass context information between different process. The `ContextCarrierRef` contains the reference of `ContextCarrier` and the `CarrierItemRef` contains the reference of CarrierItem. The `CarrierItem` instances compose a linked list. 

* Use `Tracer.inject()` to inject information of current context into carrier
* Use `Tracer.extract()` to extract info from contextCarrier. 
* Use `items()` of `ContextCarrierRef` instance to get head `CarrierItemRef` instance.  
* Use `hasNext()` of `CarrierItemRef` instance to judge if the `CarrierItemRef` has next item.
* Use `next()` of `CarrierItemRef` instance to get next item
* Use `getHeadKey` of `CarrierItemRef` instance to get key of current item
* Use `getHeadValue` of `CarrierItemRef` instance to get value of current item
* Use `setHeadValue` of `CarrierItemRef` instance to set value of current item

```java
/* 
	You can consider map as the message's header/metadata, such as Http, MQ and RPC. 
	Do the inject operation in one process and then pass the map in header/metadata.
*/
ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
Tracer.inject(contextCarrierRef);
Map<String, String> map = new HashMap<>();
CarrierItemRef next = contextCarrierRef.items();
while (next.hasNext()) {
    next = next.next();
    map.put(next.getHeadKey(), next.getHeadValue());
}
...
 
```

```java
// Receive the map representing a header/metadata and do the extract operation in another process. 
...

ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
CarrierItemRef next = contextCarrierRef.items();
for (Map.Entry<String, String> entry : map.entrySet()) {
    if (next.hasNext()) {
        next = next.next();
        if (entry.getKey().equals(next.getHeadKey()))
            next.setHeadValue(entry.getValue());
    }
}
Tracer.extract(contextCarrierRef);
```

Also, you can do the inject/extract operation when creating exit/entry span.

```java
ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
SpanRef spanRef = Tracer.createExitSpan("operationName", contextCarrierRef, "remotePeer");
Map<String, String> map = new HashMap<>();
CarrierItemRef next = contextCarrierRef.items();
while (next.hasNext()) {
    next = next.next();
    map.put(next.getHeadKey(), next.getHeadValue());
}
...

```

```java
...

ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
CarrierItemRef next = contextCarrierRef.items();
for (Map.Entry<String, String> entry : map.entrySet()) {
    if (next.hasNext()) {
        next = next.next();
        if (entry.getKey().equals(next.getHeadKey()))
            next.setHeadValue(entry.getValue());
    }
}
SpanRef spanRef = Tracer.createEntrySpan("${operationName}", contextCarrierRef);
```

# Capture/Continue Context Snapshot

* Use `Tracer.capture()` to capture the segment info and store it in `ContextSnapshotRef`, and then use `Tracer.continued()` to load the snapshot as the ref segment info. The capture/continue is used for tracing context in the x-thread tracing.

  ```java
  import org.apache.skywalking.apm.toolkit.trace.Tracer;
  ...
    
  ContextSnapshotRef contextSnapshotRef = Tracer.capture();
  Thread thread = new Thread(() -> {
      SpanRef spanRef = Tracer.createLocalSpan("${operationName}");
      Tracer.continued(contextSnapshotRef);
    	...
      
  });
  thread.start();
  thread.join();
  ```

# Add Span's Tag and Log

* Use `log` of `SpanRef` instance to record log in span

  ```java
  import org.apache.skywalking.apm.toolkit.trace.SpanRef;
  ...
  
  SpanRef spanRef = Tracer.createLocalSpan("${operationName}");
  
  // Throwable parameter
  spanRef.log(new RuntimeException("${exception_message}"));  
  
  // Map parameter
  Map<String, String> logMap = new HashMap<>();
  logMap.put("event", "${event_type}");
  logMap.put("message", "${message_value}");
  spanRef.log(logMap);  
  ```

* Use `tag` of `SpanRef` instance to add tag to span, the parameters of tag are two String which are key and value respectively.

  ```java
  import org.apache.skywalking.apm.toolkit.trace.SpanRef;
  ...
  
  SpanRef spanRef = Tracer.createLocalSpan(operationName);
  spanRef.tag("${key}", "${value}");
  ```

# Async Prepare/Finish

* Use `prepareForAsync` of `SpanRef` instance to make the span still alive until `asyncFinish` called, and then in specific time use `asyncFinish` of this `SpanRef` instance to notify this span that it could be finished.

  ```java
  import org.apache.skywalking.apm.toolkit.trace.SpanRef;
  ...
  
  SpanRef spanRef = Tracer.createLocalSpan("${operationName}");
  spanRef.prepareForAsync();
  // the span does not finish because of the prepareForAsync() operation
  Tracer.stopSpan();
  Thread thread = new Thread(() -> {
      ...
      
      spanRef.asyncFinish();
  });
  thread.start();
  thread.join();
  ```

# ActiveSpan

You can use the `ActiveSpan` to get the current span and do some operations.

* Add custom tag in the context of traced method, `ActiveSpan.tag("key", "val")`.

* `ActiveSpan.error()` Mark the current span as error status.
* `ActiveSpan.error(String errorMsg)` Mark the current span as error status with a message.
* `ActiveSpan.error(Throwable throwable)` Mark the current span as error status with a Throwable.
* `ActiveSpan.debug(String debugMsg)` Add a debug level log message in the current span.
* `ActiveSpan.info(String infoMsg)` Add an info level log message in the current span.
* `ActiveSpan.setOperationName(String operationName)` Customize an operation name. 

```java
ActiveSpan.tag("my_tag", "my_value");
ActiveSpan.error();
ActiveSpan.error("Test-Error-Reason");

ActiveSpan.error(new RuntimeException("Test-Error-Throwable"));
ActiveSpan.info("Test-Info-Msg");
ActiveSpan.debug("Test-debug-Msg");

ActiveSpan.setOperationName("${opetationName}");
```
_Sample codes only_
