## Why is `-Djava.ext.dirs` not supported?

`-Djava.ext.dirs` provides the extension class loader mechanism which was introduced in JDK 1.2, which was released in 1998.
According to [JEP 220: Modular Run-Time Images](http://openjdk.java.net/jeps/220), it ends in JDK 9, 
to simplify both the Java SE Platform and the JDK we have removed the extension mechanism, 
including the java.ext.dirs system property and the lib/ext directory.

This JEP has been applied since JDK11, which is the most active LTS JDK version. When use `-Djava.ext.dirs` in JDK11+, 
the JVM would not be able to boot with following error.
```shell
<JAVA_HOME>/lib/ext exists, extensions mechanism no longer supported; Use -classpath instead.
.Error: Could not create the Java Virtual Machine.
Error: A fatal exception has occurred. Program will exit.
```

So, SkyWalking agent would not support the extension class loader mechanism. 

### How to resolve this issue? 
If you are using JDK8 and `-Djava.ext.dirs`, follow the JRE recommendations, **Use -classpath instead**.
This should be a transparent change, which only affects your booting script.

Also, if you insist on keeping using `-Djava.ext.dirs`, the community had [a pull request](https://github.com/apache/skywalking-java/pull/19),
which leverages the bootstrap instrumentation core of the agent to support the extension class loader.

In theory, this should work, but the SkyWalking doesn't officially verify it before noticing the above JEP.
You could take it as a reference. 

The official recommendation still keeps as **Use -classpath instead**.