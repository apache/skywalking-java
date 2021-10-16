# Setup java agent

1. Agent is available for JDK 8 - 17.
1. Find `agent` folder in SkyWalking release package
1. Set `agent.service_name` in `config/agent.config`. Could be any String in English.
1. Set `collector.backend_service` in `config/agent.config`. Default point to `127.0.0.1:11800`, only works for local
   backend.
1. Add `-javaagent:/path/to/skywalking-package/agent/skywalking-agent.jar` to JVM argument. And make sure to add it
   before the `-jar` argument.

The agent release dist is included in Apache [official release](http://skywalking.apache.org/downloads/). New agent
package looks like this.

```
+-- agent
    +-- activations
         apm-toolkit-log4j-1.x-activation.jar
         apm-toolkit-log4j-2.x-activation.jar
         apm-toolkit-logback-1.x-activation.jar
         ...
    +-- config
         agent.config  
    +-- plugins
         apm-dubbo-plugin.jar
         apm-feign-default-http-9.x.jar
         apm-httpClient-4.x-plugin.jar
         .....
    +-- optional-plugins
         apm-gson-2.x-plugin.jar
         .....
    +-- bootstrap-plugins
         jdk-http-plugin.jar
         .....
    +-- logs
    skywalking-agent.jar
```

- Start your application.

## Install javaagent FAQs

- Linux Tomcat 7, Tomcat 8, Tomcat 9  
  Change the first line of `tomcat/bin/catalina.sh`.

```shell
CATALINA_OPTS="$CATALINA_OPTS -javaagent:/path/to/skywalking-agent/skywalking-agent.jar"; export CATALINA_OPTS
```

- Windows Tomcat 7, Tomcat 8, Tomcat 9  
  Change the first line of `tomcat/bin/catalina.bat`.

```shell
set "CATALINA_OPTS=-javaagent:/path/to/skywalking-agent/skywalking-agent.jar"
```

- JAR file  
  Add `-javaagent` argument to command line in which you start your app. eg:

 ```shell
 java -javaagent:/path/to/skywalking-agent/skywalking-agent.jar -jar yourApp.jar
 ```

- Jetty  
  Modify `jetty.sh`, add `-javaagent` argument to command line in which you start your app. eg:

```shell
export JAVA_OPTIONS="${JAVA_OPTIONS} -javaagent:/path/to/skywalking-agent/skywalking-agent.jar"
```

# Plugins

SkyWalking agent has supported various middlewares, frameworks and libraries. Read [supported list](Supported-list.md)
to get them and supported version. If the plugin is in **Optional²** catalog, go
to [optional plugins](Optional-plugins.md) and [bootstrap class plugin](Bootstrap-plugins.md) section to learn how to
active it.

- All plugins in `/plugins` folder are active. Remove the plugin jar, it disabled.
- The default logging output folder is `/logs`.


