# Optional Plugins
Java agent plugins are all pluggable. Optional plugins could be provided in `optional-plugins` folder under agent or 3rd party repositories.
For using these plugins, you need to put the target plugin jar file into `/plugins`.

Now, we have the following known optional plugins.
* [Plugin of tracing Spring annotation beans](agent-optional-plugins/Spring-annotation-plugin.md)
* [Plugin of tracing Oracle and Resin](agent-optional-plugins/Oracle-Resin-plugins.md)
* [Filter traces through specified endpoint name patterns](agent-optional-plugins/trace-ignore-plugin.md)
* Plugin of Gson serialization lib in optional plugin folder.
* Plugin of Zookeeper 3.4.x in optional plugin folder. The reason of being optional plugin is, many business irrelevant traces are generated, which cause extra payload to agents and backends. At the same time, those traces may be just heartbeat(s).
* [Customize enhance](Customize-enhance-trace.md) Trace methods based on description files, rather than write plugin or change source codes.
* Plugin of Spring Cloud Gateway 2.x and 3.x in optional plugin folder. Please only activate this plugin when you install agent in Spring Gateway. 
* Plugin of Spring Transaction in optional plugin folder. The reason of being optional plugin is, many local span are generated, which also spend more CPU, memory and network.
* [Plugin of Kotlin coroutine](agent-optional-plugins/Kotlin-Coroutine-plugin.md) provides the tracing across coroutines automatically. As it will add local spans to all across routines scenarios, Please assess the performance impact.
* Plugin of quartz-scheduler-2.x in the optional plugin folder. The reason for being an optional plugin is, many task scheduling systems are based on quartz-scheduler, this will cause duplicate tracing and link different sub-tasks as they share the same quartz level trigger, such as ElasticJob.
* Plugin of spring-webflux-5.x in the optional plugin folder. Please only activate this plugin when you use webflux alone as a web container. If you are using SpringMVC 5 or Spring Gateway, you don't need this plugin.
* Plugin of mybatis-3.x in optional plugin folder. The reason of being optional plugin is, many local span are generated, which also spend more CPU, memory and network.
* Plugin of sentinel-1.x in the optional plugin folder. The reason for being an optional plugin is, the sentinel plugin generates a large number of local spans, which have a potential performance impact.
* Plugin of ehcache-2.x in the optional plugin folder. The reason for being an optional plugin is, this plugin enhanced cache framework, generates large number of local spans, which have a potential performance impact.
* Plugin of guava-cache in the optional plugin folder. The reason for being an optional plugin is, this plugin enhanced cache framework, generates large number of local spans, which have a potential performance impact.
* Plugin of fastjson serialization lib in optional plugin folder.
* Plugin of jackson serialization lib in optional plugin folder.
* Plugin of Apache ShenYu(incubating) Gateway 2.4.x in optional plugin folder. Please only activate this plugin when you install agent in Apache ShenYu Gateway.
* Plugin of trace sampler CPU policy in the optional plugin folder. Please only activate this plugin when you need to disable trace collecting when the agent process CPU usage is too high(over threshold).
