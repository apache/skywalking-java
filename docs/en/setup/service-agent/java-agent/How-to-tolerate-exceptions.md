# How to tolerate custom exceptions
*In some codes, the exception is being used as a way of controlling business flow. Skywalking provides 2 ways to tolerate an exception which is traced in a span.*
1. Set the names of exception classes in the agent config
2. Use our annotation in the codes.

## Set the names of exception classes in the agent config
The property named  "statuscheck.ignored_exceptions" is used to set up class names in the agent configuration file. if the exception listed here are detected in the agent, the agent core would flag the related span as the error status.

### Demo
1. A custom exception.

    - TestNamedMatchException
    ```java
    package org.apache.skywalking.apm.agent.core.context.status;
   
    public class TestNamedMatchException extends RuntimeException {
        public TestNamedMatchException() {
        }
        public TestNamedMatchException(final String message) {
            super(message);
        }
        ...
    }
    ```
    - TestHierarchyMatchException
    ```java
    package org.apache.skywalking.apm.agent.core.context.status;
    
    public class TestHierarchyMatchException extends TestNamedMatchException {
        public TestHierarchyMatchException() {
        }
        public TestHierarchyMatchException(final String message) {
            super(message);
        }
        ...
    }
    ```
2. When the above exceptions traced in some spans, the status is like the following.

     The traced exception | Final span status |
     ----------- | ---------- |
     `org.apache.skywalking.apm.agent.core.context.status.TestNamedMatchException`  | true |
     `org.apache.skywalking.apm.agent.core.context.status.TestHierarchyMatchException`  | true |
3. After set these class names through "statuscheck.ignored_exceptions", the status of spans would be changed.

    ```
    statuscheck.ignored_exceptions=org.apache.skywalking.apm.agent.core.context.status.TestNamedMatchException
    ```

     The traced exception | Final span status |
     ----------- | ---------- |
     `org.apache.skywalking.apm.agent.core.context.status.TestNamedMatchException`  | false |
     `org.apache.skywalking.apm.agent.core.context.status.TestHierarchyMatchException`  | false |

## Use our annotation in the codes.
If an exception has the `@IgnoredException` annotation, the exception wouldn't be marked as error status when tracing. Because the annotation supports inheritance, also affects the subclasses.

### Dependency
* Dependency the toolkit, such as using maven or gradle. Since 8.2.0.
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-trace</artifactId>
      <version>{project.release.version}</version>
   </dependency>
```
### Demo
 1. A custom exception.
 
     ```java
    package org.apache.skywalking.apm.agent.core.context.status;
    
    public class TestAnnotatedException extends RuntimeException {
        public TestAnnotatedException() {
        }
        public TestAnnotatedException(final String message) {
            super(message);
        }
        ...
    }
    ```
 2. When the above exception traced in some spans, the status is like the following.
 
      The traced exception | Final span status |
      ----------- | ---------- |
      `org.apache.skywalking.apm.agent.core.context.status.TestAnnotatedException`  | true |

 3. However, when the exception annotated with the annotation, the status would be changed.

    ```java
    package org.apache.skywalking.apm.agent.core.context.status;
    
    @IgnoredException
    public class TestAnnotatedException extends RuntimeException {
    
        public TestAnnotatedException() {
        }
        public TestAnnotatedException(final String message) {
            super(message);
        }
        ...    
    }
    ```
    
     The traced exception | Final span status |
      ----------- | ---------- |
      `org.apache.skywalking.apm.agent.core.context.status.TestAnnotatedException`  | false |

## Recursive check
Due to the wrapper nature of Java exceptions, sometimes users need recursive checking. Skywalking also supports it. 

```
statuscheck.max_recursive_depth=${SW_STATUSCHECK_MAX_RECURSIVE_DEPTH:1}
```

The following report shows the benchmark results of the exception checks with different recursive depths,

```
# JMH version: 1.33
# VM version: JDK 1.8.0_292, OpenJDK 64-Bit Server VM, 25.292-b10
# VM invoker: /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/bin/java
# VM options: -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=54972:/Applications/IntelliJ IDEA.app/Contents/bin -Dfile.encoding=UTF-8
# Blackhole mode: full + dont-inline hint (default, use -Djmh.blackhole.autoDetect=true to auto-detect)
# Warmup: 5 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Average time, time/op
Benchmark                                             Mode  Cnt   Score   Error  Units
HierarchyMatchExceptionBenchmark.depthOneBenchmark    avgt   25  31.050 ± 0.731  ns/op
HierarchyMatchExceptionBenchmark.depthTwoBenchmark    avgt   25  64.918 ± 2.537  ns/op
HierarchyMatchExceptionBenchmark.depthThreeBenchmark  avgt   25  89.645 ± 2.556  ns/op
```

According to the reported results above, the exception check time is nearly proportional to the recursive depth being set.
For each single check, it costs about ten of nanoseconds (~30 nanoseconds in the report, but may vary according 
to different hardware and platforms).

Typically, we don't recommend setting this more than 10, which could cause a performance issue. 
Negative value and 0 would be ignored, which means all exceptions would make the span tagged in error status.
