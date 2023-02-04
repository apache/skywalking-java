## How to make SkyWalking agent works in `OSGI` environment?

`OSGI` implements its own set of [modularity](https://www.osgi.org/resources/modularity/), which means that each `Bundle` has its own unique class loader for isolating different versions of classes.
By default, OSGI runtime uses the boot classloader for the bundle codes, which makes the `java.lang.NoClassDefFoundError` exception in the booting stage.
```
java.lang.NoClassDefFoundError: org/apache/skywalking/apm/agent/core/plugin/interceptor/enhance/EnhancedInstance
	at ch.qos.logback.classic.Logger.buildLoggingEventAndAppend(Logger.java:419)
	at ch.qos.logback.classic.Logger.filterAndLog_0_Or3Plus(Logger.java:383)
	at ch.qos.logback.classic.Logger.log(Logger.java:765)
	at org.apache.commons.logging.impl.SLF4JLocationAwareLog.error(SLF4JLocationAwareLog.java:216)
	at org.springframework.boot.SpringApplication.reportFailure(SpringApplication.java:771)
	at org.springframework.boot.SpringApplication.handleRunFailure(SpringApplication.java:748)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:314)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1118)
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1107)
	at by.kolodyuk.osgi.springboot.SpringBootBundleActivator.start(SpringBootBundleActivator.java:21)
	at org.apache.felix.framework.util.SecureAction.startActivator(SecureAction.java:849)
	at org.apache.felix.framework.Felix.activateBundle(Felix.java:2429)
	at org.apache.felix.framework.Felix.startBundle(Felix.java:2335)
	at org.apache.felix.framework.Felix.setActiveStartLevel(Felix.java:1566)
	at org.apache.felix.framework.FrameworkStartLevelImpl.run(FrameworkStartLevelImpl.java:297)
	at java.base/java.lang.Thread.run(Thread.java:829)
```

### How to resolve this issue?
1. we need to set the parent classloader in `OSGI` to `AppClassLoader`, through the specific parameter `org.osgi.framework.bundle.parent=app`.
   The list of parameters can be found in the [OSGI API](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.api.html)
2. Load the `SkyWalking` related classes to the bundle parent class loader, `AppClassLoader`, with the parameter `org.osgi.framework.bootdelegation=org.apache.skywalking.apm.*`
   or `org.osgi.framework.bootdelegation=*`. This step is optional. Some OSGi implementations (i.e. Equinox) enable them by default

