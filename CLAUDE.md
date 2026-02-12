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
- See `apm-sniffer/apm-sdk-plugin/CLAUDE.md` for plugin development guide

**2. Bootstrap Plugins** (`apm-sniffer/bootstrap-plugins/`)
- Load at JVM bootstrap phase for JDK-level instrumentation
- Examples: jdk-threading, jdk-http, jdk-httpclient, jdk-virtual-thread-executor
- See `apm-sniffer/bootstrap-plugins/CLAUDE.md` for bootstrap plugin guide

**3. Optional Plugins** (`apm-sniffer/optional-plugins/`)
- Not included by default, user must copy to plugins directory

**4. Optional Reporter Plugins** (`apm-sniffer/optional-reporter-plugins/`)
- Alternative data collection backends (e.g., Kafka)

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
- See `apm-sniffer/apm-sdk-plugin/CLAUDE.md` for full test framework documentation

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
See `apm-sniffer/apm-sdk-plugin/CLAUDE.md` for detailed guide.

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

1. **Always check submodules**: Protocol changes may require submodule updates
2. **Generate sources first**: Run `mvnw compile` before analyzing generated code
3. **Respect checkstyle**: No System.out, no @author, no Chinese characters
4. **Use Lombok**: Prefer annotations over boilerplate code
5. **Test both unit and E2E**: Different test patterns for different scopes
6. **Java version compatibility**: Agent core must maintain Java 8 compatibility, but individual plugins may target higher JDK versions (e.g., jdk-httpclient-plugin for JDK 11+, virtual-thread plugins for JDK 21+)
7. **For plugin development**: See `apm-sniffer/apm-sdk-plugin/CLAUDE.md` and `apm-sniffer/bootstrap-plugins/CLAUDE.md`
