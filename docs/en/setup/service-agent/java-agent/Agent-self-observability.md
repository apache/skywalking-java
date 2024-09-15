# Agent Self Observability
The Java Agent self-observability feature is built-in and used to measure the tracing performance and error statistics of plugins.

It reports meters to SkyWalking oap through native meter protocol, OAP receives and analyzes meters, 
which are ultimately presented on the [Java Agent self-observability dashboard](https://skywalking.apache.org/docs/main/next/en/setup/backend/dashboards-so11y-java-agent/).

***Note: Java Agent self-observability dashboard is available since OAP 10.1.0***

# Details of agent so11y meters
- `created_tracing_context_counter` - Counter. The number of created tracing contexts. This includes a label=created_by(value=sampler,propagated). `created_by=propagated` means the agent created the context due to downstream service added sw8 header to trigger force sampling. `created_by=sampler` means the agent created this context by local sampler no matter which policy it uses.
- `finished_tracing_context_counter` - Counter. The number of finished contexts. The gap between `finished_tracing_context_counter` and `created_tracing_context_counter` should be relatively stable, otherwise, the memory cost would be increased.
- `created_ignored_context_counter` and `finished_ignored_context_counter`. Same concepts like `*_tracing_context_counter`.
- `interceptor_error_counter` - Counter. The number of errors happened in the interceptor logic, with `label=plugin_name, inter_type(constructor, inst, static)`. We don't add interceptor names into labels in case of OOM. The number of plugins is only dozens, it is predictable, but the number of interceptors will be hundreds.
- `possible_leaked_context_counter` - Counter. The number of detected leaked contexts. It should include the `label=source(value=tracing, ignore)`. When `source=tracing`, it is today's shadow tracing context. But now, it is measured.
- `tracing_context_performance` - Histogram. For successfully finished tracing context, it measures every interceptor's time cost(by using nanoseconds), the buckets of the histogram are {1000, 10000, 50000, 100000, 300000, 500000,
  1000000, 5000000, 10000000, 20000000, 50000000, 100000000}ns. This provides the performance behavior for the tracing operations.