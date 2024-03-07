# Optional Plugins
Java agent plugins are all pluggable. Optional plugins could be provided in `optional-plugins` and `expired-plugins` folder under agent or 3rd party repositories.
For using these plugins, you need to put the target plugin jar file into `/plugins`.

Now, we have the following known 2 kinds of optional plugins.

## Optional Level 2 Plugins
These plugins affect the performance or must be used under some conditions, from experiences. 
So only released in `/optional-plugins` or `/bootstrap-plugins`, copy to `/plugins` in order to make them work.

* [Plugin of tracing Spring annotation beans](agent-optional-plugins/Spring-annotation-plugin.md)
* [Plugin of tracing Oracle and Resin](agent-optional-plugins/Oracle-Resin-plugins.md)
* [Filter traces through specified endpoint name patterns](agent-optional-plugins/trace-ignore-plugin.md)
* Plugin of Gson serialization lib in optional plugin folder.
* Plugin of Zookeeper 3.4.x in optional plugin folder. The reason of being optional plugin is, many business irrelevant traces are generated, which cause extra payload to agents and backends. At the same time, those traces may be just heartbeat(s).
* [Customize enhance](Customize-enhance-trace.md) Trace methods based on description files, rather than write plugin or change source codes.
* Plugin of Spring Cloud Gateway 2.x and 3.x and 4.x in optional plugin folder. Please only activate this plugin when you install agent in Spring Gateway.
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
* Plugin for Spring 6.x and RestTemplate 6.x are in the optional plugin folder. Spring 6 requires Java 17 but SkyWalking is still compatible with Java 8. So, we put it in the optional plugin folder.
* Plugin of nacos-client 2.x lib in optional plugin folder. The reason is many business irrelevant traces are generated, which cause extra payload to agents and backends, also spend more CPU, memory and network.
* Plugin of netty-http 4.1.x lib in optional plugin folder. The reason is some frameworks use Netty HTTP as kernel, which could double the unnecessary spans and create incorrect RPC relative metrics.

## Optional Level 3 Plugins. Expired Plugins
These plugins are not tested in the CI/CD pipeline, as the previous added tests are not able to run according to the latest
CI/CD infrastructure limitations, lack of maintenance, or dependencies/images not available(e.g. removed from DockerHub). 

**Warning, there is no guarantee of working and maintenance. The committer team may remove them from the agent package 
in the future without further notice.** 

* Plugin of Spring Impala 2.6.x was tested through parrot-stream released images. The images are not available since Mar. 2024. This plugin is expired due to lack of testing.