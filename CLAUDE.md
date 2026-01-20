# CLAUDE.md - AI Assistant Guide for Apache SkyWalking Java Agent

This file provides guidance for AI assistants working with the Apache SkyWalking Java Agent codebase.

## Project Overview

Apache SkyWalking Java Agent is a Java-based APM (Application Performance Monitoring) agent designed for microservices, cloud-native, and container-based architectures. It provides automatic instrumentation for distributed tracing, performance metrics collection, and context propagation across service boundaries using bytecode manipulation via ByteBuddy.

## Repository Structure

```
skywalking-java/
├── apm-commons/                    # Shared utilities and libraries
│   ├── apm-datacarrier/            # Data buffering and transport
│   └── apm-util/                   # Common utilities
├── apm-protocol/                   # Protocol definitions
│   └── apm-network/                # gRPC protocol (submodule: skywalking-data-collect-protocol)
├── apm-sniffer/                    # Core agent and plugins (MAIN MODULE)
│   ├── apm-agent/                  # Main agent bootstrap and premain entry
│   ├── apm-agent-core/             # Core agent logic, instrumentation engine
│   ├── apm-sdk-plugin/             # Standard SDK plugins (70+ plugins)
│   ├── bootstrap-plugins/          # Bootstrap-level plugins (JDK-level)
│   ├── optional-plugins/           # Optional framework plugins
│   ├── optional-reporter-plugins/  # Reporter plugins (Kafka, etc.)
│   ├── apm-toolkit-activation/     # Toolkit activations
│   ├── apm-test-tools/             # Testing utilities
│   ├── bytebuddy-patch/            # ByteBuddy patches
│   └── config/                     # Default agent configurations
├── apm-application-toolkit/        # Public API for applications
│   ├── apm-toolkit-trace/          # Tracing API
│   ├── apm-toolkit-log4j-1.x/      # Log4j 1.x integration
│   ├── apm-toolkit-log4j-2.x/      # Log4j 2.x integration
│   ├── apm-toolkit-logback-1.x/    # Logback integration
│   ├── apm-toolkit-meter/          # Meter API
│   └── apm-toolkit-opentracing/    # OpenTracing API
├── apm-checkstyle/                 # Code style configuration
│   ├── checkStyle.xml              # Checkstyle rules
│   └── importControl.xml           # Import control rules
├── test/                           # Testing infrastructure
│   ├── plugin/                     # Plugin E2E tests
│   │   ├── scenarios/              # Test scenarios (100+ scenarios)
│   │   ├── agent-test-tools/       # Mock collector, test utilities
│   │   ├── runner-helper/          # Test runner
│   │   └── containers/             # Docker test containers
│   └── e2e/                        # End-to-end tests
├── docs/                           # Documentation
├── tools/                          # Build and utility tools
├── skywalking-agent/               # Built agent distribution output
├── changes/                        # Changelog
└── dist-material/                  # Distribution materials
```

## Build System

### Prerequisites
- JDK 8, 11, 17, 21, or 25
- Maven 3.6+
- Git (with submodule support)

### Common Build Commands

```bash
# Clone with submodules
git clone --recurse-submodules https://github.com/apache/skywalking-java.git

# Or initialize submodules after clone
git submodule init && git submodule update

# Full build with tests
./mvnw clean install

# Build without tests (recommended for development)
./mvnw clean package -Dmaven.test.skip=true

# CI build with javadoc verification
./mvnw clean verify install javadoc:javadoc

# Run checkstyle only
./mvnw checkstyle:check

# Build with submodule update
./mvnw clean package -Pall

# Docker build
make build
make docker
```

### Maven Profiles
- `all`: Includes git submodule update for protocol definitions

### Key Build Properties
- ByteBuddy: 1.17.6 (bytecode manipulation)
- gRPC: 1.74.0 (communication protocol)
- Netty: 4.1.124.Final (network framework)
- Protobuf: 3.25.5 (protocol buffers)
- Lombok: 1.18.42 (annotation processing)

## Architecture & Key Concepts

### Agent Architecture
The agent uses ByteBuddy for bytecode manipulation at runtime:

1. **Premain Entry**: `apm-agent/` contains the agent bootstrap via Java's `-javaagent` mechanism
2. **Instrumentation Engine**: `apm-agent-core/` handles class transformation and plugin loading
3. **Plugins**: Define which classes/methods to intercept and how to collect telemetry

### Plugin Categories

**1. SDK Plugins** (`apm-sniffer/apm-sdk-plugin/`)
- Framework-specific instrumentations (70+ plugins)
- Examples: grpc-1.x, spring, dubbo, mybatis, mongodb, redis, etc.
- Pattern: One directory per library/framework version

**2. Bootstrap Plugins** (`apm-sniffer/bootstrap-plugins/`)
- Load at JVM bootstrap phase for JDK-level instrumentation
- Examples: jdk-threading, jdk-http, jdk-httpclient, jdk-virtual-thread-executor

**3. Optional Plugins** (`apm-sniffer/optional-plugins/`)
- Not included by default, user must copy to plugins directory

**4. Optional Reporter Plugins** (`apm-sniffer/optional-reporter-plugins/`)
- Alternative data collection backends (e.g., Kafka)

### Plugin Instrumentation APIs (v1 vs v2)

The agent provides two instrumentation APIs. **V2 is recommended** for all new plugins; v1 is legacy and should only be used for maintaining existing plugins.

#### V2 API (Recommended)

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
        // Create span and store in context for later use
        AbstractSpan span = ContextManager.createLocalSpan("operationName");
        context.setContext(span);  // Pass to afterMethod/handleMethodException
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret, MethodInvocationContext context) {
        // Retrieve span from context
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

#### V1 API (Legacy)

V1 uses `MethodInterceptResult` only in `beforeMethod` and has no shared context between phases. **Only use for maintaining existing legacy plugins.**

**Key V1 classes (legacy):**
- `ClassEnhancePluginDefine`
- `ClassInstanceMethodsEnhancePluginDefine`
- `ClassStaticMethodsEnhancePluginDefine`
- `InstanceMethodsAroundInterceptor`
- `StaticMethodsAroundInterceptor`

### Plugin Development Rules

#### Class Matching Restrictions

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

#### Witness Classes/Methods

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

#### Bootstrap Instrumentation

For JDK core classes (rt.jar), override `isBootstrapInstrumentation()`:
```java
@Override
public boolean isBootstrapInstrumentation() {
    return true;
}
```
**WARNING**: Use bootstrap instrumentation only where absolutely necessary.

#### Plugin Configuration

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

#### Dependency Management

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

### Tracing Concepts

#### Span Types
- **EntrySpan**: Service provider/endpoint (HTTP server, MQ consumer)
- **LocalSpan**: Internal method (no remote calls)
- **ExitSpan**: Client call (HTTP client, DB access, MQ producer)

#### SpanLayer (required for EntrySpan/ExitSpan)
- `DB`: Database access
- `RPC_FRAMEWORK`: RPC calls (not ordinary HTTP)
- `HTTP`: HTTP calls
- `MQ`: Message queue
- `UNKNOWN`: Default

#### Context Propagation
- **ContextCarrier**: Cross-process propagation (serialize to headers/attachments)
- **ContextSnapshot**: Cross-thread propagation (in-memory, no serialization)

#### Required Span Attributes
For EntrySpan and ExitSpan, always set:
```java
span.setComponent(ComponentsDefine.YOUR_COMPONENT);
span.setLayer(SpanLayer.HTTP);  // or DB, MQ, RPC_FRAMEWORK
```

#### Special Tags for OAP Analysis
| Tag | Purpose |
|-----|---------|
| `http.status_code` | HTTP response code (integer) |
| `db.type` | Database type (e.g., "sql", "redis") |
| `db.statement` | SQL/query statement (enables slow query analysis) |
| `cache.type`, `cache.op`, `cache.cmd`, `cache.key` | Cache metrics |
| `mq.queue`, `mq.topic` | MQ metrics |

### Meter Plugin APIs

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

### Data Flow
1. Agent attaches to JVM via `-javaagent` flag
2. ByteBuddy transforms target classes at load time
3. Interceptors collect span/trace data on method entry/exit
4. Data is buffered via DataCarrier
5. gRPC reporter sends data to OAP backend

## Code Style & Conventions

### Checkstyle Rules (enforced via `apm-checkstyle/checkStyle.xml`)

**Prohibited patterns:**
- No `System.out.println` - use proper logging
- No `@author` tags - ASF projects don't use author annotations
- No Chinese characters in source files
- No tab characters (use 4 spaces)
- No star imports (`import xxx.*`)
- No unused or redundant imports

**Required patterns:**
- `@Override` annotation required for overridden methods
- `equals()` and `hashCode()` must be overridden together
- Apache 2.0 license header on all source files

**Naming conventions:**
- Constants/static variables: `UPPER_CASE_WITH_UNDERSCORES`
- Package names: `org.apache.skywalking.apm.*` or `test.apache.skywalking.apm.*`
- Type names: `PascalCase`
- Local variables/parameters/members: `camelCase`
- Plugin directories: `{framework}-{version}-plugin`
- Instrumentation classes: `*Instrumentation.java`
- Interceptor classes: `*Interceptor.java`

**File limits:**
- Max file length: 3000 lines

### Lombok Usage
Use Lombok annotations for boilerplate code:
- `@Getter`, `@Setter`, `@Data`
- `@Builder`
- `@Slf4j` for logging

## Testing

### Test Frameworks
- JUnit 4.12 for unit tests
- Mockito 5.0.0 for mocking

### Test Categories

**Unit Tests** (in each module's `src/test/java`)
- Standard JUnit tests
- Pattern: `*Test.java`

**Plugin E2E Tests** (`test/plugin/scenarios/`)
- 100+ test scenarios for plugin validation
- Docker-based testing with actual frameworks
- Pattern: `{framework}-{version}-scenario`

**End-to-End Tests** (`test/e2e/`)
- Full system integration testing

### Running Tests
```bash
# Unit tests
./mvnw test

# Full verification including checkstyle
./mvnw clean verify

# Skip tests during build
./mvnw package -Dmaven.test.skip=true
```

### Plugin Test Framework

The plugin test framework verifies plugin functionality using Docker containers with real services and a mock OAP backend.

#### Environment Requirements
- MacOS/Linux
- JDK 8+
- Docker & Docker Compose

#### Test Case Structure

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

#### Key Configuration Files

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

#### Running Plugin Tests Locally

```bash
# Run a specific scenario
bash ./test/plugin/run.sh -f {scenario_name}

# IMPORTANT: Rebuild agent if apm-sniffer code changed
./mvnw clean package -DskipTests -pl apm-sniffer

# Use generator to create new test case
bash ./test/plugin/generator.sh
```

#### Adding Tests to CI

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

#### Test Code Package Naming
- Test code: `org.apache.skywalking.apm.testcase.*`
- Code to be instrumented: `test.org.apache.skywalking.apm.testcase.*`

## Git Submodules

The project uses submodules for protocol definitions:
- `apm-protocol/apm-network/src/main/proto` - skywalking-data-collect-protocol

Always use `--recurse-submodules` when cloning or update submodules manually:
```bash
git submodule init && git submodule update
```

## IDE Setup (IntelliJ IDEA)

1. Import as Maven project
2. Run `./mvnw compile -Dmaven.test.skip=true` to generate protobuf sources
3. Mark generated source folders:
   - `*/target/generated-sources/protobuf/java`
   - `*/target/generated-sources/protobuf/grpc-java`
4. Enable annotation processing for Lombok

## Key Files for Understanding the Codebase

- `apm-sniffer/apm-agent/` - Agent entry point (premain)
- `apm-sniffer/apm-agent-core/src/main/java/.../enhance/` - Instrumentation engine
- `apm-sniffer/apm-agent-core/src/main/java/.../plugin/` - Plugin loading system
- `apm-sniffer/apm-sdk-plugin/` - All standard plugins (reference implementations)
- `apm-sniffer/config/agent.config` - Default agent configuration

## Common Development Tasks

### Adding a New Plugin
1. Create directory in `apm-sniffer/apm-sdk-plugin/{framework}-{version}-plugin/`
2. Implement instrumentation class using **V2 API** (e.g., extend `ClassInstanceMethodsEnhancePluginDefineV2`)
3. Implement interceptor class using **V2 API** (e.g., implement `InstanceMethodsAroundInterceptorV2`)
4. Register plugin in `skywalking-plugin.def` file
5. Add test scenario in `test/plugin/scenarios/`

### Adding an Optional Plugin
1. Create in `apm-sniffer/optional-plugins/`
2. Update documentation in `docs/en/setup/service-agent/java-agent/Optional-plugins.md`

### Modifying Agent Configuration
1. Edit `apm-sniffer/config/agent.config`
2. Update documentation if adding new options

## Documentation

- `docs/en/setup/service-agent/java-agent/` - Main agent documentation
- `docs/en/setup/service-agent/java-agent/Plugin-list.md` - Complete plugin list
- `docs/en/setup/service-agent/java-agent/Optional-plugins.md` - Optional plugins guide
- `CHANGES.md` - Changelog (update when making changes)

## Community

- GitHub Issues: https://github.com/apache/skywalking-java/issues
- Mailing List: dev@skywalking.apache.org
- Slack: #skywalking channel at Apache Slack

## Submitting Pull Requests

### Branch Strategy
- **Never work directly on main branch**
- Create a new branch for your changes

### PR Template
Follow `.github/PULL_REQUEST_TEMPLATE` based on change type:
- **Bug fix**: Add unit test, explain bug cause and fix
- **New plugin**: Add test case, component ID in OAP, logo in UI repo
- **Performance improvement**: Add benchmark with results, link to theory/discussion
- **New feature**: Link design doc if non-trivial, update docs, add tests

### PR Requirements
- Follow Apache Code of Conduct
- Include updated documentation for new features
- Include tests for new functionality
- Reference original issue (e.g., "Resolves #123")
- Update `CHANGES.md` for user-facing changes
- Pass all CI checks (checkstyle, tests, license headers)

### PR Description
- Bug fixes: Explain the bug and how it's fixed, add regression test
- New features: Link to design doc if non-trivial, update docs, add tests
- Do NOT add AI assistant as co-author

## CI/CD

GitHub Actions workflows:
- **CI**: Multi-OS (Ubuntu, macOS, Windows), Multi-Java (8, 11, 17, 21, 25)
- **Plugin Tests**: Parallel E2E tests for all plugins
- **E2E Tests**: Full system integration
- **Docker Publishing**: Multi-variant images

## Tips for AI Assistants

1. **Use V2 instrumentation API**: Always use V2 classes (`ClassEnhancePluginDefineV2`, `InstanceMethodsAroundInterceptorV2`) for new plugins; V1 is legacy
2. **NEVER use `.class` references**: In instrumentation definitions, always use string literals for class names (e.g., `byName("com.example.MyClass")` not `byName(MyClass.class.getName())`)
3. **Always set component and layer**: For EntrySpan and ExitSpan, always call `setComponent()` and `setLayer()`
4. **Prefer `byName` for class matching**: Avoid `byHierarchyMatch` unless necessary (causes performance issues)
5. **Use witness classes for version-specific plugins**: Implement `witnessClasses()` or `witnessMethods()` to activate plugins only for specific library versions
6. **Always check submodules**: Protocol changes may require submodule updates
7. **Generate sources first**: Run `mvnw compile` before analyzing generated code
8. **Respect checkstyle**: No System.out, no @author, no Chinese characters
9. **Follow plugin patterns**: Use existing V2 plugins as templates
10. **Use Lombok**: Prefer annotations over boilerplate code
11. **Test both unit and E2E**: Different test patterns for different scopes
12. **Plugin naming**: Follow `{framework}-{version}-plugin` convention
13. **Shaded dependencies**: Core dependencies are shaded to avoid classpath conflicts
14. **Java version compatibility**: Agent core must maintain Java 8 compatibility, but individual plugins may target higher JDK versions (e.g., jdk-httpclient-plugin for JDK 11+, virtual-thread plugins for JDK 21+)
15. **Bootstrap instrumentation**: Only use for JDK core classes, and only when absolutely necessary
16. **Register plugins**: Always add plugin definition to `skywalking-plugin.def` file
