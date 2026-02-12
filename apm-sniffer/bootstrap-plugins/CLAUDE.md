# CLAUDE.md - Bootstrap Plugin Development Guide

This guide covers developing bootstrap-level plugins in `apm-sniffer/bootstrap-plugins/`.

For general plugin development concepts (V2 API, tracing, class matching, testing), see `apm-sniffer/apm-sdk-plugin/CLAUDE.md`.

## What Are Bootstrap Plugins?

Bootstrap plugins instrument JDK core classes (rt.jar / java.base module) at the JVM bootstrap phase. They are loaded before application classes and can intercept fundamental JDK APIs.

**Examples:**
- `jdk-threading-plugin` - Thread pool context propagation
- `jdk-http-plugin` - `HttpURLConnection` instrumentation
- `jdk-httpclient-plugin` - JDK 11+ `HttpClient` instrumentation
- `jdk-virtual-thread-executor-plugin` - JDK 21+ virtual thread support
- `jdk-forkjoinpool-plugin` - `ForkJoinPool` instrumentation

## Key Difference from SDK Plugins

Bootstrap plugins **must** override `isBootstrapInstrumentation()`:
```java
@Override
public boolean isBootstrapInstrumentation() {
    return true;
}
```

**WARNING**: Use bootstrap instrumentation only where absolutely necessary. It affects JDK core classes and has broader impact than SDK plugins.

## Development Rules

All general plugin development rules apply (see `apm-sdk-plugin/CLAUDE.md`), plus:

1. **Use V2 API** for new bootstrap plugins, same as SDK plugins
2. **Minimal scope**: Only intercept what is strictly necessary in JDK classes
3. **Performance critical**: Bootstrap plugins run on core JDK paths - keep interceptor logic lightweight
4. **Class loading awareness**: JDK core classes are loaded by the bootstrap classloader; be careful with class references that might not be visible at bootstrap level

## Testing Bootstrap Plugins

Bootstrap plugin test scenarios use `runningMode: with_bootstrap` in `configuration.yml`:
```yaml
type: jvm
entryService: http://localhost:8080/case
healthCheck: http://localhost:8080/health
startScript: ./bin/startup.sh
runningMode: with_bootstrap
withPlugins: jdk-threading-plugin-*.jar
```

This tells the test framework to load the plugin at the bootstrap level instead of the normal plugin directory.

See `apm-sdk-plugin/CLAUDE.md` for full test framework documentation.
