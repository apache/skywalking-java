---
name: new-plugin
description: Develop a new SkyWalking Java agent plugin — instrumentation, interceptor, tracing/meter, tests, and all boilerplate
user-invocable: true
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent
---

# SkyWalking Java Agent Plugin Development

Develop a new plugin for the Apache SkyWalking Java Agent. Ask the user what library/framework to instrument and what to observe (tracing, metrics, or both), then generate all required files.

## Step 0 - Gather Requirements

Ask the user:
1. **Target library/framework** and version range (e.g., "Jedis 3.x-4.x", "Spring Kafka 2.7+")
2. **Observation type**: tracing plugin, meter plugin, or both
3. **Plugin category**: SDK plugin (default), bootstrap plugin, or optional plugin
4. **Span type needed**: Entry (server/consumer), Exit (client/producer), Local (internal), or combination

If the user already provided this info, skip asking.

## Step 1 - Understand the Library and Identify Interception Points

This is the most critical step. Do NOT jump to picking method names. Follow these phases in order.

### Phase 1: Understand How the Library Is Used

Read the target library's documentation, quickstart guides, or sample code. Understand the **user-facing API** — how developers create clients, make calls, and handle responses. This tells you:
- What objects are long-lived (clients, connections, pools) vs. per-request (requests, commands)
- Where configuration lives (server address, credentials, timeouts)
- Whether the library is sync, async, or reactive
- Whether it uses callbacks, futures, or blocking calls

Example thought process for a Redis client:
```
User creates: RedisClient client = RedisClient.create("redis://localhost:6379");
User connects: StatefulRedisConnection conn = client.connect();
User executes: conn.sync().get("key");  // or conn.async().get("key")
```
This tells you: connection holds the server address, commands are executed on the connection.

### Phase 2: Trace the Execution Flow

Starting from the user-facing API, trace inward through the library source code to understand the execution workflow:
1. What happens when the user calls the API method?
2. Where does the request object get built?
3. Where is the actual network I/O or dispatch?
4. Where is the response available?
5. For RPC/MQ: where are headers/metadata accessible for inject/extract?

**Key question at each point:** What data is directly accessible as method arguments, return values, or fields on `this`? You want interception points where you can read the data you need **without reflection**.

### Phase 3: Choose Interception Points

Pick interception points based on these principles:

**Principle 1: Data accessibility without reflection.**
Choose methods where the information you need (peer address, operation name, request/response details, headers for inject/extract) is directly available as method arguments, return values, or accessible through the `this` object's public API. **Never use reflection to read private fields.** If the data is not accessible at one method, look at a different point in the execution flow.

**Principle 2: Use `EnhancedInstance` dynamic field to propagate context inside the library.**
This is the primary mechanism for passing data between interception points. The agent adds a dynamic field to every enhanced class via `EnhancedInstance`. Use it to:
- Store server address (peer) at connection/client creation time, retrieve it at command execution time
- Store request info at request-build time, retrieve it at send time
- Pass span references from the initiating method to the completion callback

**Do NOT use `Map` or other caches to store per-instance context.** Always use the dynamic field on the relevant `EnhancedInstance`. Maps introduce memory leaks, concurrency issues, and are slower than the direct field access that `EnhancedInstance` provides.

**Principle 3: Intercept the minimal set of methods.**
Prefer one well-chosen interception point over many surface-level ones. If a library has 20 command methods that all flow through a single `dispatch()` method internally, intercept `dispatch()` — not all 20.

**Principle 4: Pick points where you can do inject/extract for cross-process propagation.**
For RPC/HTTP/MQ plugins, you need to inject trace context into outgoing requests (ExitSpan) or extract from incoming requests (EntrySpan). The interception point MUST be where headers/metadata are writable (inject) or readable (extract). If headers are not accessible at the execution method, look for:
- A request builder/decorator stage where headers can be added
- A channel/transport layer where metadata is attached
- A message properties object accessible from the method arguments

**Principle 5: Consider the span lifecycle across threads.**
If the library dispatches work asynchronously:
- Identify where the work is submitted (original thread) and where the result arrives (callback/future thread)
- You may need interception points in both threads
- Use `EnhancedInstance` dynamic field on the task/callback/future object to carry the span or `ContextSnapshot` across the thread boundary
- Use `prepareForAsync()` / `asyncFinish()` if the span must stay open across threads

### Phase 4: Map Out the Interception Plan

Before writing code, create a clear plan listing:

| Target Class | Method/Constructor | What to Do | Data Available |
|---|---|---|---|
| `XxxClient` | constructor | Store peer address in dynamic field | host, port from args |
| `XxxConnection` | `execute(Command)` | Create ExitSpan, inject carrier into command headers | command name, peer from dynamic field |
| `XxxResponseHandler` | `onComplete(Response)` | Set response tags, stop span or asyncFinish | status code, error from args |

For each interception point, verify:
- [ ] The data I need is readable from method args, `this`, or `EnhancedInstance` dynamic field — no reflection needed
- [ ] For inject: I can write headers/metadata through a public API on a method argument
- [ ] For extract: I can read headers/metadata through a public API on a method argument
- [ ] The `this` object (or a method argument) will be enhanced as `EnhancedInstance`, so I can use the dynamic field

### Choosing Span Type

| Scenario | Span Type | Requires |
|----------|-----------|----------|
| Receiving requests (HTTP server, MQ consumer, RPC provider) | EntrySpan | Extract ContextCarrier from incoming headers |
| Making outgoing calls (HTTP client, DB, cache, MQ producer, RPC consumer) | ExitSpan | Peer address; inject ContextCarrier into outgoing headers (for RPC/HTTP/MQ) |
| Internal processing (annotation-driven, local logic) | LocalSpan | Nothing extra |

### For Meter Plugins

Meter plugins follow the same understand-then-intercept process, but the goal is to find objects that expose numeric state:
- **Gauges**: Intercept the creation of pool/executor/connection-manager objects. Register a `MeterFactory.gauge()` with a supplier lambda that calls the object's own getter methods (e.g., `pool.getActiveCount()`). Store the gauge reference in the dynamic field if needed.
- **Counters**: Intercept execution methods and call `counter.increment()` on each invocation.
- **Histograms**: Intercept methods where duration or size is computable (measure between before/after, or read from response).

### Check Existing Plugins for Reference

Before writing a new plugin, check if a similar library already has a plugin:
```
apm-sniffer/apm-sdk-plugin/          # 70+ standard plugins
apm-sniffer/optional-plugins/         # Optional plugins
apm-sniffer/bootstrap-plugins/        # JDK-level plugins
```

Similar libraries often share execution patterns. Study how an existing plugin for a similar library solved the same problems — especially how it chains dynamic fields across multiple interception points and where it does inject/extract.

### Verify Against Actual Source Code — Never Speculate

**This applies to both new plugin development AND extending existing plugins to newer library versions.**

When assessing whether a plugin works with a new library version, or when choosing interception points for a new plugin, you MUST read the **actual source code** of the target library at the specific version. Do NOT rely on:
- Version number assumptions ("it's still 4.x so it should be compatible")
- Changelog summaries (they don't list every internal class rename or method removal)
- General knowledge about the library's public API (plugins intercept internal classes, which change without notice)

**What to verify for each intercepted class/method:**
1. Does the target class still exist at the exact FQCN? (internal classes get renamed, extracted, or removed between minor versions)
2. Does the intercepted method still exist with a compatible signature? (parameters may be added/removed/reordered)
3. Do the witness classes still correctly distinguish versions? (a witness class that exists in both old and new versions won't prevent the plugin from loading on an incompatible version)
4. Do the runtime APIs called by the interceptor still exist? (e.g., calling `cluster.getDescription()` will crash if that method was removed, even if the plugin loaded successfully)

**How to verify:**
- Fetch the actual source file from the library's Git repository at the specific version tag (e.g., `https://raw.githubusercontent.com/{org}/{repo}/{tag}/path/to/Class.java`)
- Or download the specific JAR and inspect the class
- Or add the version to the plugin's test dependencies and compile

**Real examples of why this matters:**
- MongoDB driver 4.11 removed `Cluster.getDescription()` — the plugin loads (witness classes pass) but crashes at runtime with `NoSuchMethodError`
- Feign 12.2 moved `ReflectiveFeign$BuildTemplateByResolvingArgs` to `RequestTemplateFactoryResolver$BuildTemplateByResolvingArgs` — the path variable interception silently stops working
- MariaDB 3.0 renamed every JDBC wrapper class (`MariaDbConnection` → `Connection`) — none of the plugin's `byName` matchers match anything

**When extending `support-version.list` to add newer versions:**
Before adding a version, verify that every class and method the plugin intercepts still exists in that version's source. A plugin test passing does not mean everything works — it only means the test scenario's specific code path exercised the intercepted methods. Missing interception points may go undetected if the test doesn't cover them.

## Step 2 - Create Plugin Module

### Directory Structure

**SDK plugin** (most common):
```
apm-sniffer/apm-sdk-plugin/{framework}-{version}-plugin/
  pom.xml
  src/main/java/org/apache/skywalking/apm/plugin/{framework}/v{N}/
    define/
      {Target}Instrumentation.java          # One per target class
    {Target}Interceptor.java                # One per interception concern
    {Target}ConstructorInterceptor.java     # If intercepting constructors
    {PluginName}PluginConfig.java           # If plugin needs configuration
  src/main/resources/
    skywalking-plugin.def                   # Plugin registration
  src/test/java/org/apache/skywalking/apm/plugin/{framework}/v{N}/
    {Target}InterceptorTest.java            # Unit tests
```

**Bootstrap plugin** (for JDK classes):
```
apm-sniffer/bootstrap-plugins/{name}-plugin/
  (same structure, but instrumentation class overrides isBootstrapInstrumentation)
```

**Optional plugin**:
```
apm-sniffer/optional-plugins/{name}-plugin/
  (same structure as SDK plugin)
```

### pom.xml Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.apache.skywalking</groupId>
        <artifactId>apm-sdk-plugin</artifactId>  <!-- or bootstrap-plugins / optional-plugins -->
        <version>${revision}</version>
    </parent>

    <artifactId>{framework}-{version}-plugin</artifactId>
    <packaging>jar</packaging>

    <properties>
        <target-library.version>X.Y.Z</target-library.version>
    </properties>

    <dependencies>
        <!-- Target library - MUST be provided scope -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>target-library</artifactId>
            <version>${target-library.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**CRITICAL dependency rules:**
- Target library: **always `provided` scope** (supplied by the application at runtime)
- `apm-agent-core`: inherited from parent POM as `provided`
- `apm-util`: inherited from parent POM as `provided`
- Never bundle target library classes into the plugin JAR
- If the plugin needs a 3rd-party utility not already in agent-core, discuss with maintainers first

### Register in Parent POM

Add the new module to the parent `pom.xml`:
```xml
<modules>
    ...
    <module>{framework}-{version}-plugin</module>
</modules>
```

## Step 3 - Implement Instrumentation Class (V2 API)

**ALWAYS use V2 API for new plugins.** V1 is legacy.

### Import Rules (Enforced by Checkstyle)

Plugins may ONLY import from:
- `java.*` - Java standard library
- `org.apache.skywalking.*` - SkyWalking modules
- `net.bytebuddy.*` - ByteBuddy (for matchers in instrumentation classes)

**No other 3rd-party imports are allowed in instrumentation/activation files.** This is enforced by `apm-checkstyle/importControl.xml`. Interceptor classes CAN reference target library classes (they're loaded after the target library).

### Class Matching

**CRITICAL: NEVER use `.class` references in instrumentation definitions.** Always use string literals.

```java
// WRONG - breaks agent if class doesn't exist at runtime
byName(SomeThirdPartyClass.class.getName())
takesArgument(0, SomeThirdPartyClass.class)

// CORRECT - safe string literals
byName("com.example.SomeThirdPartyClass")
takesArgumentWithType(0, "com.example.SomeThirdPartyClass")
```

Available ClassMatch types (from `org.apache.skywalking.apm.agent.core.plugin.match`):

| Matcher | Usage | Performance |
|---------|-------|-------------|
| `NameMatch.byName(String)` | Exact class name | Best (HashMap lookup) |
| `MultiClassNameMatch.byMultiClassMatch(String...)` | Multiple exact names | Good |
| `HierarchyMatch.byHierarchyMatch(String...)` | Implements interface / extends class | Expensive - avoid unless necessary |
| `ClassAnnotationMatch.byClassAnnotationMatch(String...)` | Has annotation(s) | Moderate |
| `MethodAnnotationMatch.byMethodAnnotationMatch(String...)` | Has method with annotation | Moderate |
| `PrefixMatch.nameStartsWith(String...)` | Class name prefix | Moderate |
| `RegexMatch.byRegexMatch(String...)` | Regex on class name | Expensive |
| `LogicalMatchOperation.and(match1, match2)` | AND composition | Depends on operands |
| `LogicalMatchOperation.or(match1, match2)` | OR composition | Depends on operands |

**Prefer `NameMatch.byName()` whenever possible.** It uses a fast HashMap lookup. All other matchers require linear scanning.

### Method Matching (ByteBuddy ElementMatcher API)

Common matchers from `net.bytebuddy.matcher.ElementMatchers`:

```java
// By name
named("methodName")

// By argument count
takesArguments(2)
takesArguments(0)  // no-arg methods

// By argument type (use SkyWalking's helper - string-based, safe)
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
takesArgumentWithType(0, "com.example.SomeType")  // arg at index 0

// By return type (SkyWalking helper)
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ReturnTypeNameMatch.returnsWithType;
returnsWithType("java.util.List")

// By annotation (SkyWalking helper - string-based)
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.AnnotationTypeNameMatch.isAnnotatedWithType;
isAnnotatedWithType("org.springframework.web.bind.annotation.RequestMapping")

// Visibility
isPublic()
isPrivate()

// Composition
named("execute").and(takesArguments(1))
named("method1").or(named("method2"))
not(isDeclaredBy(Object.class))

// Match any (use sparingly)
any()
```

### Instrumentation Template - Instance Methods

```java
package org.apache.skywalking.apm.plugin.xxx.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.InstanceMethodsInterceptV2Point;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.ClassInstanceMethodsEnhancePluginDefineV2;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class XxxInstrumentation extends ClassInstanceMethodsEnhancePluginDefineV2 {

    private static final String ENHANCE_CLASS = "com.example.TargetClass";
    private static final String INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.xxx.XxxInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;  // null or empty array if not intercepting constructors
    }

    @Override
    public InstanceMethodsInterceptV2Point[] getInstanceMethodsInterceptV2Points() {
        return new InstanceMethodsInterceptV2Point[] {
            new InstanceMethodsInterceptV2Point() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("targetMethod");
                }

                @Override
                public String getMethodsInterceptorV2() {
                    return INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
```

### Instrumentation Template - Static Methods

```java
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.ClassStaticMethodsEnhancePluginDefineV2;

public class XxxStaticInstrumentation extends ClassStaticMethodsEnhancePluginDefineV2 {
    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[] {
            new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("factoryMethod").and(takesArguments(2));
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }
}
```

### Instrumentation Template - Both Instance + Static Methods

Extend `ClassEnhancePluginDefineV2` and implement all four methods:
- `enhanceClass()`
- `getConstructorsInterceptPoints()`
- `getInstanceMethodsInterceptV2Points()`
- `getStaticMethodsInterceptPoints()`

### Matching Interface/Abstract Class Implementations

Use `HierarchyMatch` when you need to intercept all implementations of an interface:

```java
import org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch;

@Override
protected ClassMatch enhanceClass() {
    return HierarchyMatch.byHierarchyMatch("com.example.SomeInterface");
}
```

**When to use HierarchyMatch:**
- The library has an interface/abstract class with multiple implementations
- You cannot enumerate all implementation class names
- Example: intercepting all `javax.servlet.Servlet` implementations

**Performance warning:** HierarchyMatch checks every loaded class against the hierarchy. Prefer `NameMatch` or `MultiClassNameMatch` if you know the concrete class names.

**Combining with other matchers:**
```java
import org.apache.skywalking.apm.agent.core.plugin.match.logical.LogicalMatchOperation;

@Override
protected ClassMatch enhanceClass() {
    return LogicalMatchOperation.and(
        PrefixMatch.nameStartsWith("com.example"),
        HierarchyMatch.byHierarchyMatch("java.lang.Runnable")
    );
}
```

### Witness Classes for Version Detection

Override `witnessClasses()` or `witnessMethods()` to activate the plugin only for specific library versions:

```java
@Override
protected String[] witnessClasses() {
    // Plugin only loads if this class exists in the application
    return new String[] {"com.example.VersionSpecificClass"};
}

@Override
protected List<WitnessMethod> witnessMethods() {
    return Collections.singletonList(
        new WitnessMethod("com.example.SomeClass", ElementMatchers.named("methodAddedInV2"))
    );
}
```

### Bootstrap Plugin Override

For bootstrap plugins (instrumenting JDK classes), add:
```java
@Override
public boolean isBootstrapInstrumentation() {
    return true;
}
```

**Bootstrap plugin rules:**
- Only for JDK core classes (java.*, javax.*, sun.*)
- Minimal interception scope (performance-critical paths)
- Extra care with class loading (bootstrap classloader visibility)
- Test with `runningMode: with_bootstrap`

## Step 4 - Implement Interceptor Class (V2 API)

### Available Interceptor Interfaces

| Interface | Use Case |
|-----------|----------|
| `InstanceMethodsAroundInterceptorV2` | Instance method interception |
| `StaticMethodsAroundInterceptorV2` | Static method interception |
| `InstanceConstructorInterceptor` | Constructor interception (shared V1/V2) |

### Core APIs Available in Interceptors

**ContextManager** - Central tracing API (ThreadLocal-based):

**CRITICAL threading rule:** All span lifecycle APIs (`createEntrySpan`, `createExitSpan`, `createLocalSpan`, `activeSpan`, `stopSpan`) operate on a **per-thread context via ThreadLocal**. By default, `createXxxSpan` and `stopSpan` MUST be called in the **same thread**. There are only two ways to work across threads:
1. **`ContextSnapshot` (capture/continued)** — snapshot the context in thread A, then `continued()` in thread B to link a NEW span in thread B back to the parent trace. Each thread manages its own span lifecycle independently.
2. **Async mode (`prepareForAsync`/`asyncFinish`)** — keeps a single span alive beyond the creating thread. Call `prepareForAsync()` in the original thread (before `stopSpan`), then `asyncFinish()` from any thread when the async work completes. Between `prepareForAsync` and `asyncFinish`, you may call tag/log/error on the span from any thread, but you must NOT call `ContextManager.stopSpan()` for that span again.

```java
import org.apache.skywalking.apm.agent.core.context.ContextManager;

// Create spans (must stopSpan in the SAME thread, unless async mode)
AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
AbstractSpan span = ContextManager.createLocalSpan(operationName);
AbstractSpan span = ContextManager.createExitSpan(operationName, contextCarrier, remotePeer);
AbstractSpan span = ContextManager.createExitSpan(operationName, remotePeer);

// Span lifecycle (same thread as create, unless async mode)
ContextManager.activeSpan();      // Get current span in THIS thread
ContextManager.stopSpan();        // Stop current span in THIS thread
ContextManager.isActive();        // Check if context exists in THIS thread

// Cross-process propagation (inject/extract ContextCarrier into headers/metadata)
ContextManager.inject(carrier);   // Inject into outgoing carrier
ContextManager.extract(carrier);  // Extract from incoming carrier

// Cross-thread propagation (ContextSnapshot — link spans across threads)
ContextManager.capture();         // Capture snapshot in originating thread
ContextManager.continued(snapshot); // Continue from snapshot in receiving thread

// Trace metadata
ContextManager.getGlobalTraceId();
ContextManager.getSegmentId();
ContextManager.getSpanId();
```

**AbstractSpan** - Span configuration:
```java
span.setComponent(ComponentsDefine.YOUR_COMPONENT);  // Required for Entry/Exit
span.setLayer(SpanLayer.HTTP);                        // Required for Entry/Exit
span.setOperationName("GET:/api/users");
span.setPeer("host:port");                            // Required for Exit spans

// Tags
span.tag(Tags.URL, url);
span.tag(Tags.HTTP_RESPONSE_STATUS_CODE, statusCode);
span.tag(Tags.DB_TYPE, "sql");
span.tag(Tags.DB_STATEMENT, sql);
span.tag(Tags.ofKey("custom.key"), value);

// Error handling
span.errorOccurred();
span.log(throwable);

// Async support
span.prepareForAsync();  // Must call in original thread
span.asyncFinish();      // Call in async thread when done
```

**SpanLayer** values: `DB`, `RPC_FRAMEWORK`, `HTTP`, `MQ`, `CACHE`, `GEN_AI`

**Standard Tags** (from `org.apache.skywalking.apm.agent.core.context.tag.Tags`):

| Tag                 | Constant                         | Purpose                  |
|---------------------|----------------------------------|--------------------------|
| `url`               | `Tags.URL`                       | Request URL              |
| `http.status_code`  | `Tags.HTTP_RESPONSE_STATUS_CODE` | HTTP status (IntegerTag) |
| `http.method`       | `Tags.HTTP.METHOD`               | HTTP method              |
| `db.type`           | `Tags.DB_TYPE`                   | Database type            |
| `db.instance`       | `Tags.DB_INSTANCE`               | Database name            |
| `db.statement`      | `Tags.DB_STATEMENT`              | SQL/query                |
| `db.bind_variables` | `Tags.DB_BIND_VARIABLES`         | Bound params             |
| `mq.queue`          | `Tags.MQ_QUEUE`                  | Queue name               |
| `mq.topic`          | `Tags.MQ_TOPIC`                  | Topic name               |
| `mq.broker`         | `Tags.MQ_BROKER`                 | Broker address           |
| `cache.type`        | `Tags.CACHE_TYPE`                | Cache type               |
| `cache.op`          | `Tags.CACHE_OP`                  | "read" or "write"        |
| `cache.cmd`         | `Tags.CACHE_CMD`                 | Cache command            |
| `cache.key`         | `Tags.CACHE_KEY`                 | Cache key                |
| Custom              | `Tags.ofKey("key")`              | Any custom tag           |

**EnhancedInstance** - Dynamic field for cross-interceptor data:
```java
// Store data (e.g., in constructor interceptor)
objInst.setSkyWalkingDynamicField(connectionInfo);

// Retrieve data (e.g., in method interceptor)
ConnectionInfo info = (ConnectionInfo) objInst.getSkyWalkingDynamicField();
```

**Logging** - Agent-internal logging (NOT application logging):
```java
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

private static final ILog LOGGER = LogManager.getLogger(MyInterceptor.class);
LOGGER.info("message: {}", value);
LOGGER.error("error", throwable);
```

**MeterFactory** - For meter plugins:
```java
import org.apache.skywalking.apm.toolkit.meter.MeterFactory;
import org.apache.skywalking.apm.toolkit.meter.Counter;
import org.apache.skywalking.apm.toolkit.meter.Gauge;
import org.apache.skywalking.apm.toolkit.meter.Histogram;

Counter counter = MeterFactory.counter("metric_name")
    .tag("key", "value")
    .mode(Counter.Mode.INCREMENT)
    .build();
counter.increment(1.0);

Gauge gauge = MeterFactory.gauge("metric_name", () -> pool.getActiveCount())
    .tag("pool_name", name)
    .build();

Histogram histogram = MeterFactory.histogram("metric_name")
    .steps(Arrays.asList(10.0, 50.0, 100.0, 500.0))
    .minValue(0)
    .build();
histogram.addValue(latencyMs);
```

### Interceptor Template - ExitSpan (Client/Producer)

```java
package org.apache.skywalking.apm.plugin.xxx;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

public class XxxClientInterceptor implements InstanceMethodsAroundInterceptorV2 {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInvocationContext context) throws Throwable {
        // 1. Build peer address from stored connection info
        String remotePeer = (String) objInst.getSkyWalkingDynamicField();

        // 2. Create ExitSpan with ContextCarrier for cross-process propagation
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan("operation/name", contextCarrier, remotePeer);
        span.setComponent(ComponentsDefine.YOUR_COMPONENT);
        SpanLayer.asHttp(span);  // or asDB, asMQ, asRPCFramework, asCache

        // 3. Inject trace context into outgoing request headers
        // The request object is typically one of the method arguments
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            // Set header on the outgoing request:
            // request.setHeader(next.getHeadKey(), next.getHeadValue());
        }

        // 4. Set tags
        Tags.URL.set(span, url);

        // 5. Store span in context for afterMethod
        context.setContext(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) throws Throwable {
        // Check response status, set tags/errors
        AbstractSpan span = (AbstractSpan) context.getContext();
        // Example: Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
        // if (statusCode >= 400) span.errorOccurred();

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        ContextManager.activeSpan().log(t);
        ContextManager.activeSpan().errorOccurred();
    }
}
```

### Interceptor Template - EntrySpan (Server/Consumer)

```java
public class XxxServerInterceptor implements InstanceMethodsAroundInterceptorV2 {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInvocationContext context) throws Throwable {
        // 1. Extract trace context from incoming request headers
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            // Read header from incoming request:
            // next.setHeadValue(request.getHeader(next.getHeadKey()));
        }

        // 2. Create EntrySpan (extracts context automatically)
        AbstractSpan span = ContextManager.createEntrySpan("operation/name", contextCarrier);
        span.setComponent(ComponentsDefine.YOUR_COMPONENT);
        SpanLayer.asHttp(span);  // or asMQ, asRPCFramework
        span.setPeer(clientAddress);  // Optional: client address

        context.setContext(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        ContextManager.activeSpan().log(t);
        ContextManager.activeSpan().errorOccurred();
    }
}
```

### Interceptor Template - Constructor (Store Connection Info)

```java
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

public class XxxConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        // Store connection info for later use by method interceptors
        String host = (String) allArguments[0];
        int port = (int) allArguments[1];
        objInst.setSkyWalkingDynamicField(host + ":" + port);
    }
}
```

### Cross-Thread Context Propagation (ContextSnapshot)

Use `ContextSnapshot` when the library dispatches work to another thread and you want the new thread's spans to be linked to the parent trace. Each thread creates and stops its OWN spans — the snapshot only provides the link.

```java
// Thread A (originating thread) — create span, capture snapshot, stop span (all same thread)
@Override
public void beforeMethod(..., MethodInvocationContext context) {
    AbstractSpan span = ContextManager.createLocalSpan("async/dispatch");
    // Capture context snapshot BEFORE handing off to another thread
    ContextSnapshot snapshot = ContextManager.capture();
    // Store snapshot on the task object via EnhancedInstance dynamic field
    ((EnhancedInstance) allArguments[0]).setSkyWalkingDynamicField(snapshot);
    ContextManager.stopSpan();  // Stop span in THIS thread (same thread as create)
}

// Thread B (receiving thread) — create its OWN span, link to parent via continued()
@Override
public void beforeMethod(EnhancedInstance objInst, ...) {
    ContextSnapshot snapshot = (ContextSnapshot) objInst.getSkyWalkingDynamicField();
    if (snapshot != null) {
        AbstractSpan span = ContextManager.createLocalSpan("async/execute");
        ContextManager.continued(snapshot);  // Link this span to the parent trace
    }
}

@Override
public Object afterMethod(...) {
    if (ContextManager.isActive()) {
        ContextManager.stopSpan();  // Stop span in THIS thread (same thread as create)
    }
    return ret;
}
```

### Async Span Pattern (prepareForAsync / asyncFinish)

Use this when a **single span** needs to stay open across thread boundaries — e.g., an ExitSpan created before an async call, finished when the callback fires in another thread. The key difference from ContextSnapshot: here one span lives across threads instead of each thread having its own span.

```java
// Thread A — create span, mark async, stop context (all same thread)
@Override
public void beforeMethod(..., MethodInvocationContext context) {
    AbstractSpan span = ContextManager.createExitSpan("async/call", remotePeer);
    span.setComponent(ComponentsDefine.YOUR_COMPONENT);
    SpanLayer.asHttp(span);

    span.prepareForAsync();       // Mark: this span will finish in another thread
    ContextManager.stopSpan();    // Detach from THIS thread's context (required, same thread as create)

    // Store span reference on the callback object's dynamic field
    ((EnhancedInstance) callback).setSkyWalkingDynamicField(span);
}

// Thread B (callback/completion handler) — finish the async span
@Override
public void beforeMethod(EnhancedInstance objInst, ...) {
    AbstractSpan span = (AbstractSpan) objInst.getSkyWalkingDynamicField();
    if (span != null) {
        // Add response info to the span (tag/log/error are thread-safe after prepareForAsync)
        span.tag(Tags.HTTP_RESPONSE_STATUS_CODE, statusCode);
        if (isError) span.errorOccurred();
        span.asyncFinish();  // Must match prepareForAsync count
    }
}
```

### Plugin Configuration (Optional)

```java
import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class XxxPluginConfig {
    public static class Plugin {
        @PluginConfig(root = XxxPluginConfig.class)
        public static class Xxx {
            // Config key: plugin.xxx.trace_param
            public static boolean TRACE_PARAM = false;
            // Config key: plugin.xxx.max_length
            public static int MAX_LENGTH = 256;
        }
    }
}
```

## Step 5 - Register Plugin

Create `src/main/resources/skywalking-plugin.def`:
```
plugin-name=org.apache.skywalking.apm.plugin.xxx.define.XxxInstrumentation
plugin-name=org.apache.skywalking.apm.plugin.xxx.define.XxxOtherInstrumentation
```

Format: `{plugin-id}={fully.qualified.InstrumentationClassName}` (one line per instrumentation class, all sharing the same plugin-id prefix).

## Step 6 - Write Unit Tests

### Test Setup Pattern

```java
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;

@RunWith(TracingSegmentRunner.class)
public class XxxInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EnhancedInstance enhancedInstance;

    private XxxInterceptor interceptor;

    @Before
    public void setUp() {
        interceptor = new XxxInterceptor();
        // Setup mocks
    }

    @Test
    public void testNormalRequest() throws Throwable {
        // Arrange
        Object[] allArguments = new Object[] { /* mock args */ };
        Class[] argumentsTypes = new Class[] { /* arg types */ };

        // Act
        interceptor.beforeMethod(enhancedInstance, null, allArguments, argumentsTypes, null);
        interceptor.afterMethod(enhancedInstance, null, allArguments, argumentsTypes, mockResponse, null);

        // Assert spans
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(segment);
        assertThat(spans.size(), is(1));
        // Verify span properties...
    }
}
```

## Step 7 - Write E2E Plugin Tests

### Test Scenario Structure

```
test/plugin/scenarios/{framework}-{version}-scenario/
  bin/startup.sh
  config/expectedData.yaml
  src/main/java/org/apache/skywalking/apm/testcase/{framework}/
    controller/CaseController.java    # HTTP endpoints
  pom.xml
  configuration.yml
  support-version.list
```

**When copying an existing scenario to create a new one**, update the scenario name in ALL of these files:
- `pom.xml` — `artifactId`, `name`, `finalName`
- `src/main/assembly/assembly.xml` — JAR filename reference
- `bin/startup.sh` — JAR filename in java -jar command
- `config/expectedData.yaml` — `serviceName` field AND `parentService` in refs (but NOT URL paths — those are the app context path)
- `support-version.list` — new versions
- For JDK 17+ scenarios: update `compiler.version` to `17`, `spring.boot.version` to `3.x`, change `javax.annotation` imports to `jakarta.annotation` in Java source
- Add the scenario to the appropriate CI workflow (`plugins-test.*.yaml` for JDK 8, `plugins-jdk17-test.*.yaml` for JDK 17)

### configuration.yml

```yaml
type: jvm
entryService: http://localhost:8080/{scenario-name}/case/{endpoint}
healthCheck: http://localhost:8080/{scenario-name}/case/healthCheck
startScript: ./bin/startup.sh
environment: []
dependencies: {}
```

### expectedData.yaml for Tracing

```yaml
segmentItems:
  - serviceName: {scenario-name}
    segmentSize: ge 1
    segments:
      - segmentId: not null
        spans:
          - operationName: your/operation
            parentSpanId: -1
            spanId: 0
            spanLayer: Http           # Http, DB, RPCFramework, MQ, CACHE
            spanType: Exit            # Entry, Exit, Local
            startTime: nq 0
            endTime: nq 0
            componentId: 2            # Must match ComponentsDefine ID
            isError: false
            peer: 'host:port'
            skipAnalysis: 'false'
            tags:
              - {key: url, value: not null}
              - {key: http.method, value: GET}
            logs: []
            refs: []
```

### expectedData.yaml for Meters

```yaml
meterItems:
  - serviceName: {scenario-name}
    meterSize: ge 1
    meters:
      - meterId:
          name: your_counter_name
          tags:
            - {name: tag_key, value: tag_value}
        singleValue: gt 0
```

### Assertion Operators

| Operator | Meaning |
|----------|---------|
| `eq VALUE` | Equals |
| `nq VALUE` | Not equals |
| `ge VALUE` | Greater or equal |
| `gt VALUE` | Greater than |
| `not null` | Must be present |
| `null` | Must be absent |

### Running Tests

```bash
bash ./test/plugin/run.sh -f {scenario-name}
bash ./test/plugin/run.sh --debug {scenario-name}  # Save actualData.yaml for debugging
```

**IMPORTANT: Run E2E test scenarios ONE AT A TIME.** Multiple scenarios use the same Docker ports (8080, etc.) and will conflict if run in parallel. Always wait for one scenario to finish before starting the next.

## Step 8 - Shading (Package Relocation)

Plugins automatically inherit ByteBuddy shading from the parent POM:
```xml
<relocation>
    <pattern>net.bytebuddy</pattern>
    <shadedPattern>org.apache.skywalking.apm.dependencies.net.bytebuddy</shadedPattern>
</relocation>
```

The agent-core module handles shading of all core dependencies:
- `com.google.*` -> `org.apache.skywalking.apm.dependencies.com.google.*`
- `io.grpc.*` -> `org.apache.skywalking.apm.dependencies.io.grpc.*`
- `io.netty.*` -> `org.apache.skywalking.apm.dependencies.io.netty.*`
- `org.slf4j.*` -> `org.apache.skywalking.apm.dependencies.org.slf4j.*`

**Plugins should NOT add their own shade configurations** unless they need to bundle a library not in agent-core (rare, requires maintainer approval). If a plugin needs a reporter-level dependency, see `optional-reporter-plugins/kafka-reporter-plugin/pom.xml` for the pattern.

## Step 9 - Code Style Checklist

Before submitting:
- [ ] No `System.out.println` (use `ILog` from `LogManager`)
- [ ] No `@author` tags (ASF policy)
- [ ] No Chinese characters in source
- [ ] No tab characters (use 4 spaces)
- [ ] No star imports (`import xxx.*`)
- [ ] No unused imports
- [ ] `@Override` on all overridden methods
- [ ] Apache 2.0 license header on all source files
- [ ] File length under 3000 lines
- [ ] Constants in `UPPER_SNAKE_CASE`
- [ ] Types in `PascalCase`, variables in `camelCase`
- [ ] Imports only from `java.*`, `org.apache.skywalking.*`, `net.bytebuddy.*` (in instrumentation files)
- [ ] Target library dependencies in `provided` scope
- [ ] Using V2 API for new plugins
- [ ] String literals (not `.class` references) in instrumentation definitions
- [ ] `skywalking-plugin.def` registered
- [ ] Module added to parent POM

## Step 10 - Update Documentation

After verifying the plugin works (locally or via CI), update these documentation files:

**1. `docs/en/setup/service-agent/java-agent/Supported-list.md`**
Update the version range for the relevant entry. Format example:
```
  * [MongoDB Java Driver](https://github.com/mongodb/mongo-java-driver) 2.13-2.14, 3.4.0-3.12.7, 4.0.0-4.10.2
```
For new plugins, add a new bullet under the appropriate category.

**2. `CHANGES.md`**
Add a changelog entry under the current unreleased version section. Format:
```
* Extend {plugin-name} plugin to support {library} {version-range}.
```
Or for new plugins:
```
* Add {framework} {version} plugin.
```

**3. `test/plugin/scenarios/{scenario}/support-version.list`**
Add verified versions. Only include the **latest patch version for each minor version** — do not list every patch release.

**This step is mandatory.** Documentation updates are part of the PR requirements.

## Quick Reference - Plugin Type Decision Tree

```
Is the target a JDK core class (java.*, sun.*)?
  YES -> Bootstrap plugin (isBootstrapInstrumentation = true)
  NO  -> Is it commonly used and should be auto-activated?
           YES -> SDK plugin (apm-sdk-plugin/)
           NO  -> Optional plugin (optional-plugins/)

What span type?
  Receiving requests (HTTP server, MQ consumer, RPC provider) -> EntrySpan
  Making outgoing calls (HTTP client, DB, cache, MQ producer) -> ExitSpan
  Internal processing (annotation, local logic)               -> LocalSpan

Need cross-process propagation?
  YES -> Use ContextCarrier (inject on exit, extract on entry)
  NO  -> No carrier needed

Need cross-thread propagation?
  YES -> Use ContextSnapshot (capture + continued) OR prepareForAsync/asyncFinish
  NO  -> Standard span lifecycle

Need metrics without traces?
  YES -> Meter plugin (Counter/Gauge/Histogram via MeterFactory)

Need both traces and metrics?
  YES -> Separate interceptors or combine in same interceptor
```