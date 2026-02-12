# CLAUDE.md - SDK Plugin Development Guide

This guide covers developing standard SDK plugins in `apm-sniffer/apm-sdk-plugin/`.

## Plugin Instrumentation APIs (v1 vs v2)

The agent provides two instrumentation APIs. **V2 is recommended** for all new plugins; v1 is legacy and should only be used for maintaining existing plugins.

### V2 API (Recommended)

V2 provides a `MethodInvocationContext` that is shared across all interception phases (`beforeMethod`, `afterMethod`, `handleMethodException`), allowing you to pass data (e.g., spans) between phases.

**Instrumentation class (extends `ClassEnhancePluginDefineV2`):**
```java
public class XxxInstrumentation extends ClassInstanceMethodsEnhancePluginDefineV2 {
    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName("target.class.Name");
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[] { ... };
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
                    return "org.apache.skywalking.apm.plugin.xxx.XxxInterceptor";
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

**Interceptor class (implements `InstanceMethodsAroundInterceptorV2`):**
```java
public class XxxInterceptor implements InstanceMethodsAroundInterceptorV2 {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInvocationContext context) {
        AbstractSpan span = ContextManager.createLocalSpan("operationName");
        context.setContext(span);  // Pass to afterMethod/handleMethodException
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) {
        AbstractSpan span = (AbstractSpan) context.getContext();
        span.asyncFinish();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        AbstractSpan span = (AbstractSpan) context.getContext();
        span.log(t);
    }
}
```

**Key V2 classes:**
- `ClassEnhancePluginDefineV2` - Base class for plugins with both instance and static methods
- `ClassInstanceMethodsEnhancePluginDefineV2` - For instance methods only
- `ClassStaticMethodsEnhancePluginDefineV2` - For static methods only
- `InstanceMethodsAroundInterceptorV2` - Interceptor interface with `MethodInvocationContext`
- `StaticMethodsAroundInterceptorV2` - Static method interceptor with context

### V1 API (Legacy)

V1 uses `MethodInterceptResult` only in `beforeMethod` and has no shared context between phases. **Only use for maintaining existing legacy plugins.**

**Key V1 classes (legacy):**
- `ClassEnhancePluginDefine`
- `ClassInstanceMethodsEnhancePluginDefine`
- `ClassStaticMethodsEnhancePluginDefine`
- `InstanceMethodsAroundInterceptor`
- `StaticMethodsAroundInterceptor`

## Plugin Development Rules

### Class Matching Restrictions

**CRITICAL: Never use `.class` references in instrumentation definitions:**
```java
// WRONG - will break the agent if ThirdPartyClass doesn't exist
takesArguments(ThirdPartyClass.class)
byName(ThirdPartyClass.class.getName())

// CORRECT - use string literals
takesArguments("com.example.ThirdPartyClass")
byName("com.example.ThirdPartyClass")
```

**ClassMatch options:**
- `byName(String)`: Match by full class name (package + class name) - **preferred**
- `byClassAnnotationMatch`: Match classes with specific annotations (does NOT support inherited annotations)
- `byMethodAnnotationMatch`: Match classes with methods having specific annotations
- `byHierarchyMatch`: Match by parent class/interface - **avoid unless necessary** (performance impact)

### Witness Classes/Methods

Use witness classes/methods to activate plugins only for specific library versions:
```java
@Override
protected String[] witnessClasses() {
    return new String[] { "com.example.VersionSpecificClass" };
}

@Override
protected List<WitnessMethod> witnessMethods() {
    return Collections.singletonList(
        new WitnessMethod("com.example.SomeClass", ElementMatchers.named("specificMethod"))
    );
}
```

### Plugin Configuration

Use `@PluginConfig` annotation for custom plugin settings:
```java
public class MyPluginConfig {
    public static class Plugin {
        @PluginConfig(root = MyPluginConfig.class)
        public static class MyPlugin {
            public static boolean SOME_SETTING = false;
        }
    }
}
```
Config key becomes: `plugin.myplugin.some_setting`

### Dependency Management

**Plugin dependencies must use `provided` scope:**
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>target-library</artifactId>
    <version>${version}</version>
    <scope>provided</scope>
</dependency>
```

**Agent core dependency policy:**
- New dependencies in agent core are treated with extreme caution
- Prefer using existing imported libraries already in the project
- Prefer JDK standard libraries over third-party libraries
- Plugins should rely on the target application's libraries (provided scope), not bundle them

## Tracing Concepts

### Span Types
- **EntrySpan**: Service provider/endpoint (HTTP server, MQ consumer)
- **LocalSpan**: Internal method (no remote calls)
- **ExitSpan**: Client call (HTTP client, DB access, MQ producer)

### SpanLayer (required for EntrySpan/ExitSpan)
- `DB`: Database access
- `RPC_FRAMEWORK`: RPC calls (not ordinary HTTP)
- `HTTP`: HTTP calls
- `MQ`: Message queue
- `UNKNOWN`: Default

### Context Propagation
- **ContextCarrier**: Cross-process propagation (serialize to headers/attachments)
- **ContextSnapshot**: Cross-thread propagation (in-memory, no serialization)

### Required Span Attributes
For EntrySpan and ExitSpan, always set:
```java
span.setComponent(ComponentsDefine.YOUR_COMPONENT);
span.setLayer(SpanLayer.HTTP);  // or DB, MQ, RPC_FRAMEWORK
```

### Special Tags for OAP Analysis
| Tag | Purpose |
|-----|---------|
| `http.status_code` | HTTP response code (integer) |
| `db.type` | Database type (e.g., "sql", "redis") |
| `db.statement` | SQL/query statement (enables slow query analysis) |
| `cache.type`, `cache.op`, `cache.cmd`, `cache.key` | Cache metrics |
| `mq.queue`, `mq.topic` | MQ metrics |

## Meter Plugin APIs

For collecting numeric metrics (alternative to tracing):
```java
// Counter
Counter counter = MeterFactory.counter("metric_name")
    .tag("key", "value")
    .mode(Counter.Mode.INCREMENT)
    .build();
counter.increment(1d);

// Gauge
Gauge gauge = MeterFactory.gauge("metric_name", () -> getValue())
    .tag("key", "value")
    .build();

// Histogram
Histogram histogram = MeterFactory.histogram("metric_name")
    .steps(Arrays.asList(1, 5, 10))
    .build();
histogram.addValue(3);
```

## Adding a New SDK Plugin

1. Create directory: `apm-sniffer/apm-sdk-plugin/{framework}-{version}-plugin/`
2. Implement instrumentation class using **V2 API** (extend `ClassInstanceMethodsEnhancePluginDefineV2`)
3. Implement interceptor class using **V2 API** (implement `InstanceMethodsAroundInterceptorV2`)
4. Register plugin in `skywalking-plugin.def` file
5. Add test scenario in `test/plugin/scenarios/`

## Plugin Test Framework

The plugin test framework verifies plugin functionality using Docker containers with real services and a mock OAP backend.

### Environment Requirements
- MacOS/Linux
- JDK 8+
- Docker & Docker Compose

### Test Case Structure

**JVM-container (preferred):**
```
{scenario}-scenario/
├── bin/
│   └── startup.sh              # JVM startup script (required)
├── config/
│   └── expectedData.yaml       # Expected trace/meter/log data
├── src/main/java/...           # Test application code
├── pom.xml
├── configuration.yml           # Test case configuration
└── support-version.list        # Supported versions (one per line)
```

**Tomcat-container:**
```
{scenario}-scenario/
├── config/
│   └── expectedData.yaml
├── src/main/
│   ├── java/...
│   └── webapp/WEB-INF/web.xml
├── pom.xml
├── configuration.yml
└── support-version.list
```

### Key Configuration Files

**configuration.yml:**
```yaml
type: jvm                                    # or tomcat
entryService: http://localhost:8080/case     # Entry endpoint (GET)
healthCheck: http://localhost:8080/health    # Health check endpoint (HEAD)
startScript: ./bin/startup.sh                # JVM-container only
runningMode: default                         # default|with_optional|with_bootstrap
withPlugins: apm-spring-annotation-plugin-*.jar  # For optional/bootstrap modes
environment:
  - KEY=value
dependencies:                                # External services (docker-compose style)
  mysql:
    image: mysql:8.0
    hostname: mysql
    environment:
      - MYSQL_ROOT_PASSWORD=root
```

**support-version.list:**
```
# One version per line, use # for comments
# Only include ONE version per minor version (not all patch versions)
4.3.6
4.4.1
4.5.0
```

**expectedData.yaml:**

Trace and meter expectations are typically in separate scenarios.

*For tracing plugins:*
```yaml
segmentItems:
  - serviceName: your-scenario
    segmentSize: ge 1                        # Operators: eq, ge, gt, nq
    segments:
      - segmentId: not null
        spans:
          - operationName: /your/endpoint
            parentSpanId: -1                 # -1 for root span
            spanId: 0
            spanLayer: Http                  # Http, DB, RPC_FRAMEWORK, MQ, CACHE, Unknown
            spanType: Entry                  # Entry, Exit, Local
            startTime: nq 0
            endTime: nq 0
            componentId: 1
            isError: false
            peer: ''                         # Empty string for Entry/Local, required for Exit
            skipAnalysis: false
            tags:
              - {key: url, value: not null}
              - {key: http.method, value: GET}
              - {key: http.status_code, value: '200'}
            logs: []
            refs: []                         # SegmentRefs for cross-process/cross-thread
```

*For meter plugins:*
```yaml
meterItems:
  - serviceName: your-scenario
    meterSize: ge 1
    meters:
      - meterId:
          name: test_counter
          tags:
            - {name: key1, value: value1}    # Note: uses 'name' not 'key'
        singleValue: gt 0                    # For counter/gauge
      - meterId:
          name: test_histogram
          tags:
            - {name: key1, value: value1}
        histogramBuckets:                    # For histogram
          - 0.0
          - 1.0
          - 5.0
          - 10.0
```

**startup.sh (JVM-container):**
```bash
#!/bin/bash
home="$(cd "$(dirname $0)"; pwd)"
# ${agent_opts} is REQUIRED - contains agent parameters
java -jar ${agent_opts} ${home}/../libs/your-scenario.jar &
```

### Running Plugin Tests Locally

```bash
# Run a specific scenario
bash ./test/plugin/run.sh -f {scenario_name}

# IMPORTANT: Rebuild agent if apm-sniffer code changed
./mvnw clean package -DskipTests -pl apm-sniffer

# Use generator to create new test case
bash ./test/plugin/generator.sh
```

### Adding Tests to CI

Add scenario to the appropriate `.github/workflows/` file:
- Use `python3 tools/select-group.py` to find the file with fewest cases
- **JDK 8 tests**: `plugins-test.<n>.yaml`
- **JDK 17 tests**: `plugins-jdk17-test.<n>.yaml`
- **JDK 21 tests**: `plugins-jdk21-test.<n>.yaml`
- **JDK 25 tests**: `plugins-jdk25-test.<n>.yaml`

```yaml
matrix:
  case:
    - your-scenario-scenario
```

### Test Code Package Naming
- Test code: `org.apache.skywalking.apm.testcase.*`
- Code to be instrumented: `test.org.apache.skywalking.apm.testcase.*`

## Tips for AI Assistants

1. **Use V2 instrumentation API**: Always use V2 classes for new plugins; V1 is legacy
2. **NEVER use `.class` references**: Always use string literals for class names
3. **Always set component and layer**: For EntrySpan and ExitSpan, always call `setComponent()` and `setLayer()`
4. **Prefer `byName` for class matching**: Avoid `byHierarchyMatch` unless necessary (performance impact)
5. **Use witness classes for version-specific plugins**: Implement `witnessClasses()` or `witnessMethods()`
6. **Follow plugin patterns**: Use existing V2 plugins as templates
7. **Plugin naming**: Follow `{framework}-{version}-plugin` convention
8. **Register plugins**: Always add plugin definition to `skywalking-plugin.def` file
9. **Java version compatibility**: Agent core must maintain Java 8 compatibility, but individual plugins may target higher JDK versions
10. **Shaded dependencies**: Core dependencies are shaded to avoid classpath conflicts
