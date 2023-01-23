# Use annotation to mark the method you want to trace.

* Add `@Trace` to any method you want to trace. After that, you can see the span in the Stack.
* Methods annotated with `@Tag` will try to tag the **current active span** with the given key (`Tag#key()`) and (`Tag#value()`),
  if there is no active span at all, this annotation takes no effect. `@Tag` can be repeated, and can be used in companion with `@Trace`, see examples below.
  The `value` of `Tag` is the same as what are supported in [Customize Enhance Trace](Customize-enhance-trace.md).

```java
/**
 * The codes below will generate a span,
 * and two types of tags, 
      one type tag: keys are `tag1` and `tag2`, values are the passed-in parameters, respectively, 
      the other type tag: keys are `username`  and `age`, values are the return value in User, respectively
 */
@Trace
@Tag(key = "tag1", value = "arg[0]")
@Tag(key = "tag2", value = "arg[1]")
@Tag(key = "username", value = "returnedObj.username")
@Tag(key = "age", value = "returnedObj.age")
public User methodYouWantToTrace(String param1, String param2) {
    // ...
}
```
_Sample codes only_

