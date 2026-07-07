# CLAUDE.md - Plugin Test Guide

This file guides AI assistants writing and changing **plugin tests** under `test/plugin/scenarios/`.
For the full test-framework mechanics (scenario layout, `configuration.yml`, `expectedData.yaml`,
containers, running locally) see the **"Plugin Test Framework"** section of
`apm-sniffer/apm-sdk-plugin/CLAUDE.md`, and `docs/.../Plugin-test.md`.

## Testing philosophy: plugin scenarios are the key test — not unit tests

For an agent plugin, the **plugin test scenario is the primary and sufficient test**. Do **not**
over-engineer by adding many unit tests for a plugin.

A plugin's real risk is two things, and a scenario proves both at once by running the **real
framework**:

1. **Cast / fetch-data logic** — the interceptor casting the intercepted arguments to the framework
   types and reading request/response fields off them (headers, method, URI, status, params). Mocks
   can't validate that the real framework class actually exposes those methods the way you assumed.
2. **Version compatibility** — the same interceptor must work across the supported version range.

So: **when adding or changing a plugin, add or extend its scenario**, not a pile of mock-based unit
tests. Reserve unit tests for genuinely logic/namespace-critical **shared** code where a deterministic
in-JVM assertion earns its keep (e.g. `servlet-commons` `wrap()` resolving javax vs jakarta with both
APIs on the test classpath) — never for per-plugin interceptor mocking.

## Validation path: assert the full span shape your plugin produces

A scenario proves a plugin by asserting the **exact spans** the real framework makes the agent
produce. Pick the shape by what the framework actually does — and don't settle for a single entry
span when the plugin also does propagation, or you silently skip the inject/extract path.

- **RPC / web frameworks that have both a server and a client side** (Struts, Spring MVC, gRPC,
  Dubbo, HTTP servers …): drive the full round-trip —
  `curl → server → its own client call → another service's endpoint`. The trace then contains
  **2 entry spans + 1 exit span**:
  1. the **server entry** span for the request `curl` hit (proves the server side: extract + entry),
  2. the **client exit** span for the outbound call (proves inject + exit),
  3. a **second entry** span on the other endpoint carrying a **cross-process ref** back to the
     first (proves propagation across the wire — the receiving/extract side).

  **The ref itself must be asserted** — for RPC the cross-process `refs:` block is the whole point,
  so `expectedData.yaml` must include it on that second entry span with `refType: CrossProcess`,
  `parentEndpoint`, `parentSpanId` (pointing at the exit span), `parentTraceSegmentId: not null`,
  `parentService`, and `traceId: not null`. A scenario that asserts spans but not the ref does not
  prove propagation.

  The struts2.7 scenario is exactly this: `case.action` (entry) → HttpClient (exit) → `case1.action`
  (entry + ref); jetty-12 uses the same shape via a JDK-HttpURLConnection self-call. Keep that nested
  self-call — it is the only thing that proves inject / extract / propagation; a lone entry span does not.

- **Client-only plugins** (JDBC/PostgreSQL, Redis, MongoDB, an HTTP client, a message producer): the
  peer is a database/broker, not another instrumented SkyWalking service, so there is **no second
  entry span**. Assert just the **client exit** span (component, `peer`, and the `db.*` / `http.*`
  tags). You do NOT need to stand up another service endpoint.

## Version coverage: one version per minor (latest patch)

`support-version.list` must cover the framework's supported range, but keep **one version per minor
version — the latest patch**, not every patch. E.g. Jetty `12.0.36` + `12.1.10`; Struts `7.0.3`,
`7.1.1`, `7.2.1`; Spring `6.0.4`, `6.1.1`, `6.2.19`. Verify each version resolves on Maven Central.
This is the compatibility proof; CI runs the scenario against each listed version.

## How the test containers work (and where the JDK/Tomcat version really comes from)

A scenario runs inside one of two prebuilt container images, selected by `type:` in
`configuration.yml`:

- **`type: jvm`** → the **`agent-test-jvm`** container. Your app is a fat-jar started by
  `bin/startup.sh` with `${agent_opts}` (the `-javaagent:` line — you MUST include it in
  `startup.sh`). Built `FROM ${base_image_java}`.
- **`type: tomcat`** → the **`agent-test-tomcat`** container. Your app is a `*.war` dropped into
  `/usr/local/tomcat/webapps/`; the agent is wired in via the container's patched `catalina.sh`.
  Built `FROM ${base_image_tomcat}`.

**The version pin is in the CI lane, not `configuration.yml`.** `configuration.yml` only says
`type: tomcat` — it does **not** choose a Tomcat version. The concrete JDK and Tomcat versions come
from the `base_image_java` / `base_image_tomcat` inputs passed to `./.github/actions/build` in the
lane's `Build` job, and **every scenario in that lane shares that one image**. So:

- `type: tomcat` on the `plugins-jdk8-*` lane → `tomcat:8.5-jdk8` (javax);
  on `plugins-jdk11-*` → `tomcat:9.0-jdk11` (javax);
  on `plugins-jdk17-*` → `tomcat:10.1-jdk17` (**jakarta**).
- That is why a jakarta/Servlet-6 framework (Struts 7, Jetty 12, Spring 6) MUST be registered on a
  JDK-17 lane — it needs the Tomcat-10.1 base — and a javax one on an older lane. Putting a jakarta
  WAR on a Tomcat-8.5 lane just won't deploy.

To run locally you pass the same two knobs explicitly (the defaults are the javax/JDK-8 pair):
`bash test/plugin/run.sh --base_image_java eclipse-temurin:17-jdk --base_image_tomcat tomcat:10.1-jdk17-temurin <scenario>`.
Some newer bases lack `curl` (the container health check needs it) — the JDK-25 lane bakes it in
via `.github/workflows/Dockerfile-tomcat-jdk25-withCurl`.

## How the GitHub Actions lanes work

Scenarios are load-balanced across `.github/workflows/plugins-<jdk>-test.<group>.yaml` files. Each
file is one lane: its `Build` job pins `base_image_java` + `base_image_tomcat`, and its `test` job's
`matrix.case:` list names the scenarios that run on that image. Lanes exist per JDK
(`jdk8`/`jdk11`/`jdk17`/`jdk21`/`jdk25`); the trailing `.0`/`.1`/… is just a bucket to spread load.
Register a new scenario by adding its directory name to the `matrix.case:` list of the lane whose
base image matches the framework's needs — use `python3 tools/select-group.py` to pick the
least-loaded bucket in that JDK lane.

## Cold-start and the 3s entry-service timeout (a real gotcha)

The container's `run.sh` runs under `set -e` and hits the entry service with
`curl -s --max-time 3 ${SCENARIO_ENTRY_SERVICE}` — **no warm-up first**. So the very first (cold)
entry request must finish within **3 seconds** or `curl` exits 28 and the whole scenario fails. That
first request pays a lot of one-time cost at once: the agent's ByteBuddy enhancement of every class
it touches on that path, JIT, and — for WARs — a cold Jasper **JSP compile**, all under the
container's tight `-Xmx256m` heap and whatever CPU the runner gives, with **jacoco** simultaneously
instrumenting the `org.apache.skywalking.*` classes for coverage.

Keep the entry path's cold cost low:

- Don't render a **JSP** from the entry action if a lightweight result works (e.g. Struts
  `httpheader`, a plain-text/stream result). The view is irrelevant to what a plugin scenario
  verifies (entry/exit spans, tags, propagation), and a cold Jasper compile is expensive.
- Be extra careful when the entry action makes a **nested internal call** (for cross-process
  propagation coverage): if that nested request also does cold work (a JSP compile) *while* the
  outer request is still cold-enhancing Struts/HttpClient, the two cold-start storms contend for the
  limited CPU and heap and blow up **super-linearly** (observed ~0.5s each in isolation but ~3–20s
  nested under throttle). This is a slow first request, not a plugin bug — verify with a
  CPU-throttled container (`docker run --cpus=0.5 …`) before assuming a hang.

## Practical notes

- One scenario per plugin (or per javax/jakarta era). Mirror an existing sibling scenario as a
  template and change only what differs (framework version, namespace, deployment model).
- Match `type:` to the deployment: `jvm` (fat-jar started by `bin/startup.sh`, `${agent_opts}`
  required) or `tomcat` (WAR on a container). Jakarta/newer frameworks go in a JDK-17 CI group.
- Assert the entry span shape that exercises the plugin's data logic: `componentId`, `spanType: Entry`,
  `spanLayer: Http`, and the `url` / `http.method` / `http.status_code` (and `http.params` /
  `http.headers` where the plugin collects them) tags, plus cross-process refs.
- Register the scenario in the right `.github/workflows/plugins-*.yaml` matrix (use
  `python3 tools/select-group.py` to pick the least-loaded group for its JDK lane).
