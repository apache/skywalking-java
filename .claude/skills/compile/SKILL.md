---
name: compile
description: Build the SkyWalking Java agent — full build, skip tests, single module, or plugin test scenarios
user-invocable: true
allowed-tools: Bash, Read, Glob, Grep
---

# Compile SkyWalking Java Agent

Build the project based on user request. Detect what they want to build and run the appropriate command.

## Prerequisites

- JDK 17, 21, or 25 (JDK 8 is supported at runtime but JDK 17+ is needed to compile)
- Maven is bundled as `./mvnw` (Maven wrapper)
- Git submodules must be initialized for protocol definitions

Check JDK version first:
```bash
java -version
```

If submodules are not initialized:
```bash
git submodule init && git submodule update
```

## Build Commands

### Full build (with tests)
```bash
./mvnw clean package -Pall
```

### Full build (skip tests — recommended for development)
```bash
./mvnw clean package -Dmaven.test.skip=true
```

### CI build (with javadoc verification)
```bash
./mvnw clean verify install javadoc:javadoc -Dmaven.test.skip=true
```

### Build a single plugin module
```bash
./mvnw clean package -pl apm-sniffer/apm-sdk-plugin/{plugin-name} -am -Dmaven.test.skip=true
```
The `-am` flag builds required dependencies. Replace `{plugin-name}` with the actual plugin directory name.

### Run checkstyle only
```bash
./mvnw checkstyle:check
```

### Run unit tests for a single module
```bash
./mvnw test -pl apm-sniffer/apm-sdk-plugin/{plugin-name}
```

### Build agent distribution only (after full build)
The built agent is in `skywalking-agent/` directory after a full build.

### Run a plugin E2E test scenario

The E2E test framework has a **two-phase build** (matching CI):

**Phase 1 — Build agent + test tools + Docker images (one-time setup):**
```bash
# Build the agent (JDK 17+ required)
./mvnw clean package -Dmaven.test.skip=true

# Switch to JDK 8 to build test tools and Docker images
# The test/plugin/pom.xml builds: runner-helper, agent-test-tools, JVM/Tomcat container images
export JAVA_HOME=$(/usr/libexec/java_home -v 8)
export PATH=$JAVA_HOME/bin:$PATH

./mvnw --batch-mode -f test/plugin/pom.xml \
  -Dmaven.test.skip \
  -Dbase_image_java=eclipse-temurin:8-jdk \
  -Dbase_image_tomcat=tomcat:8.5-jdk8-openjdk \
  -Dcontainer_image_version=1.0.0 \
  clean package
```

This builds `skywalking/agent-test-jvm:1.0.0` and `skywalking/agent-test-tomcat:1.0.0` Docker images,
plus `test/plugin/dist/plugin-runner-helper.jar` and `test/plugin/agent-test-tools/dist/` (mock-collector, validator).

**Phase 2 — Run test scenarios (per scenario):**
```bash
# Use JDK 8 (matching CI). JDK 17 works for runner-helper but JDK 8 matches CI exactly.
export JAVA_HOME=$(/usr/libexec/java_home -v 8)
export PATH=$JAVA_HOME/bin:$PATH

# Run WITHOUT -f (reuses pre-built tools and images from Phase 1)
bash ./test/plugin/run.sh --debug {scenario-name}
```

**IMPORTANT flags:**
- `--debug` — keeps workspace with logs and `actualData.yaml` for inspection after test
- `-f` (force) — rebuilds ALL test tools and Docker images from scratch. **Do NOT use** if Phase 1 already completed — it re-clones `skywalking-agent-test-tool` from GitHub and rebuilds everything, which is slow and may fail due to network issues.
- Without `-f` — reuses existing tools/images. This is the normal way to run tests.

**Key rules:**
- Run scenarios **one at a time** — they share Docker ports (8080, etc.) and will conflict if parallel
- JDK 8 test scenarios use `eclipse-temurin:8-jdk` base image
- JDK 17 test scenarios (in `plugins-jdk17-test` workflows) use `eclipse-temurin:17-jdk` base image
- After a test, check `test/plugin/workspace/{scenario}/{version}/data/actualData.yaml` vs `expectedData.yaml` for debugging failures
- Check `test/plugin/workspace/{scenario}/{version}/logs/` for container logs

**Diagnosing "startup script not exists" failures:**
This error means the scenario ZIP wasn't built or copied into the container. The root cause is almost always a **silent Maven build failure** — `run.sh` uses `mvnw -q` (quiet mode) which hides errors. Common causes:
1. **Maven Central network timeout** — downloading a new library version fails silently. The `mvnw clean package` exits non-zero but the `-q` flag hides the error, and `run.sh` continues with missing artifacts.
2. **Docker Hub timeout** — pulling dependency images (mongo, mysql, kafka, zookeeper) fails with EOF/TLS errors.
3. **Killed previous run** — if a prior parallel run was killed mid-execution, leftover state in `test/plugin/workspace/` can interfere. Always `rm -rf test/plugin/workspace/{scenario}` before rerunning.

To debug: run the Maven build manually in the scenario directory with verbose output:
```bash
cd test/plugin/scenarios/{scenario-name}
../../../../mvnw clean package -Dtest.framework.version={version} -Dmaven.test.skip=true
```
If this succeeds but `run.sh` fails, it's likely a transient Maven Central network issue. Pre-download dependencies first:
```bash
# Pre-warm Maven cache for all versions before running tests
for v in $(grep -v '^#' test/plugin/scenarios/{scenario}/support-version.list | grep -v '^$'); do
  cd test/plugin/scenarios/{scenario}
  ../../../../mvnw dependency:resolve -Dtest.framework.version=$v -q
  cd -
done
```

**Pre-pulling Docker dependency images:**
Scenarios with `dependencies:` in `configuration.yml` need external Docker images. Pre-pull them before running tests to avoid mid-test Docker Hub failures:
```bash
# Check what images a scenario needs
grep "image:" test/plugin/scenarios/{scenario}/configuration.yml
# Pull them
docker pull {image:tag}
```

### Generate protobuf sources (needed before IDE import)
```bash
./mvnw compile -Dmaven.test.skip=true
```
Then mark `*/target/generated-sources/protobuf/java` and `*/target/generated-sources/protobuf/grpc-java` as generated source folders in your IDE.

## Common Issues

- **Submodule not initialized**: If proto files are missing, run `git submodule init && git submodule update`
- **Wrong JDK version**: Agent build requires JDK 17+. Test tools build (test/plugin/pom.xml) works best with JDK 8. Check with `java -version`.
- **Checkstyle failures**: Run `./mvnw checkstyle:check` to see violations. Common: star imports, unused imports, System.out.println, missing @Override.
- **Test scenario Docker issues**: Ensure Docker daemon is running. Use `--debug` flag to inspect `actualData.yaml`.
- **`run.sh -f` fails on agent-test-tools**: The `-f` flag clones `skywalking-agent-test-tool` from GitHub and rebuilds from source. If GitHub is slow or unreachable, this fails. Solution: run Phase 1 build separately (see above), then use `run.sh` without `-f`.
- **Lombok errors in runner-helper on JDK 25**: The test framework uses Lombok 1.18.20 which doesn't support JDK 25. Use JDK 8 or JDK 17 for building and running test tools.
- **"startup script not exists" inside container**: The scenario ZIP wasn't built or copied correctly. Check that `mvnw clean package` succeeds in the scenario directory and produces both a `.jar` and `.zip` in `target/`.
- **Port conflicts**: Never run multiple E2E scenarios simultaneously — they all bind to the same Docker ports.
