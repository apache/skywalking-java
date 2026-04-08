# Claude Code Skills

[Claude Code](https://claude.ai/claude-code) is an AI-powered CLI tool by Anthropic. This project includes
custom **skills** (`.claude/skills/`) that teach Claude Code how to work with the SkyWalking Java Agent codebase.

Skills are reusable prompt templates that Claude Code can invoke via slash commands. They encode project-specific
knowledge so that common development tasks can be performed consistently and correctly.

## Available Skills

### `/new-plugin` — Develop a New Plugin

Guides the full lifecycle of creating a new SkyWalking Java agent plugin:

1. **Gather requirements** — target library, observation type (tracing/meter), span types
2. **Identify interception points** — understand library usage, trace execution flow, choose classes/methods
3. **Create plugin module** — directory structure, pom.xml, dependencies (`provided` scope)
4. **Implement instrumentation** — V2 API, class matching (ByteBuddy), method matching
5. **Implement interceptors** — ContextManager spans, ContextCarrier inject/extract, EnhancedInstance dynamic fields
6. **Register plugin** — `skywalking-plugin.def`
7. **Write unit tests** — TracingSegmentRunner, SegmentStorage
8. **Write E2E tests** — Docker-based scenarios, expectedData.yaml
9. **Code style** — checkstyle compliance, import restrictions
10. **Update documentation** — Supported-list.md, CHANGES.md

Key principles encoded in this skill:
- Always use **V2 API** (`ClassEnhancePluginDefineV2`, `InstanceMethodsAroundInterceptorV2`)
- **Never use `.class` references** in instrumentation — always string literals
- **Never use reflection** to access private fields — choose interception points with accessible data
- **Never use Maps** to cache per-instance context — use `EnhancedInstance.setSkyWalkingDynamicField()`
- **Verify actual source code** of target libraries — never speculate from version numbers
- Span lifecycle APIs are **ThreadLocal-based** — create/stop in same thread unless async mode

### `/compile` — Build the Project

Runs the appropriate build command based on what you need:
- Full build with or without tests
- Single module build
- Checkstyle check
- Plugin E2E test scenarios
- Protobuf source generation for IDE setup

## How to Use

1. Install [Claude Code](https://docs.anthropic.com/en/docs/claude-code/overview)
2. Navigate to the `skywalking-java` repository root
3. Run `claude` to start Claude Code
4. Type `/new-plugin` or `/compile` to invoke a skill

Skills can also be triggered implicitly — when you describe a task that matches a skill's purpose,
Claude Code may suggest or invoke it automatically.

## Project Context Files

In addition to skills, the project includes `CLAUDE.md` files that provide codebase context:

| File | Purpose |
|------|---------|
| `CLAUDE.md` (root) | Project overview, build system, architecture, conventions |
| `apm-sniffer/apm-sdk-plugin/CLAUDE.md` | SDK plugin development guide (V2 API, class matching, testing) |
| `apm-sniffer/bootstrap-plugins/CLAUDE.md` | Bootstrap plugin specifics (JDK class instrumentation) |

These files are automatically loaded by Claude Code when working in the repository, providing it with
the knowledge needed to assist with development tasks.
