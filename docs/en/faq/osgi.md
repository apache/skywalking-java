## Why `Skywalking` doesn't work in `OSGI` environment?
In a normal Java application, our application code is usually loaded directly with `AppClassLoader`, when it is started with `-javaagent:<agent_root>\skywalking-agent.jar`,
The code in `agent` is also loaded by `AppClassLoader`, while `Skywalking Plugins` is loaded by `AgentClassLoader`.
The parent class loader of `AgentClassLoader` is the class loader of the application code, which is `AppClassLoader`, so the code related to `Skywalking` is running in `AppClassLoader`.

But `OSGI` implements its own set of [modularity](https://www.osgi.org/resources/modularity/), which means that each `Bundle` has its own unique class loader for isolating different versions of classes.
In `OSGI` start `agent` for class conversion interception, at this time the class loader of the application code is the unique class loader of the bundle, when the class related to `Skywalking` is needed
will throw `java.lang.ClassNotFoundException` exception. Resulting in a startup exception

### How to resolve this issue?
1. we need to adjust the father of the class loader in `OSGI` to `AppClassLoader`, with the specific parameter `org.osgi.framework.bundle.parent=app`, because the default parent class of each bundle is boot
   The list of parameters can be found in the [OSGI API](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.api.html)
2. Load the `Skywalking` related classes to the bundle parent class loader, `AppClassLoader`, with the parameter `org.osgi.framework.bootdelegation=org.apache.skywalking.apm.*`
   or `org.osgi.framework.bootdelegation=*`, This step is optional,some OSGi implementations (i.e. Equinox) enable them by default

