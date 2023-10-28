# Re-transform Support

By default, the SkyWalking agent is not expected to install alongside other Java agents.
Therefore, in cases where class re-transformation occurs, the instrumentation provided by 
SkyWalking could potentially be overwritten or lost.

The limitation of the default type description strategy, **HYBRID**, is described as following.
> A description type strategy represents a type as a TypeDescription.ForLoadedType if a 
retransformation or redefinition is applied on a type. Using a loaded type typically results in better performance 
as no I/O is required for resolving type descriptions. However, any interaction with the type is carried out via the Java reflection API. 
Using the reflection API triggers eager loading of any type that is part of a method or field signature. 
If any of these types are missing from the class path, this eager loading will cause a NoClassDefFoundError. 
Some Java code declares optional dependencies to other classes which are only realized if the optional dependency is present. 
Such code relies on the Java reflection API not being used for types using optional dependencies.

In certain scenarios, users may need to have multiple agents installed.
To accommodate this, they should enable this new feature by setting this configuration to true.

```
# Enables the Java agent kernel to run in a mode that supports class re-transformation by other agents.
# It's recommended to use this mode only if the service has more than one SkyWalking agent installed.
#
# Be aware that this mode can slow down class loading. As a result, you might experience a higher
# startup delay (the time it takes for the program to launch and be ready for use).
agent.enable_retransform_support={SW_ENABLE_RETRANSFORM_SUPPORT:false}
```

This option would change the type description strategy to `POOL_FIRST`.
But, **doing so can cause overhead as processing loaded types is supported very efficiently by a JVM.**
and slow down class loading. As a result, you might experience a higher 
startup delay (the time it takes for the program to launch and be ready for use).

Based on our tests, enabling this feature could potentially double the additional time cost
incurred by the agent during the boot-up stage. This might also lead to delays in class 
loading during runtime.