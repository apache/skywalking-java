## How to make SkyWalking agent works in `OSGI` environment?

`OSGI` implements its own set of [modularity](https://www.osgi.org/resources/modularity/), which means that each `Bundle` has its own unique class loader for isolating different versions of classes.
By default, OSGI runtime uses the boot classloader for the bundle codes, which makes the `java.lang.ClassNotFoundException` exception in the booting stage.


### How to resolve this issue?
1. we need to set the parent classloader in `OSGI` to `AppClassLoader`, through the specific parameter `org.osgi.framework.bundle.parent=app`.
   The list of parameters can be found in the [OSGI API](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.api.html)
2. Load the `SkyWalking` related classes to the bundle parent class loader, `AppClassLoader`, with the parameter `org.osgi.framework.bootdelegation=org.apache.skywalking.apm.*`
   or `org.osgi.framework.bootdelegation=*`. This step is optional. Some OSGi implementations (i.e. Equinox) enable them by default

