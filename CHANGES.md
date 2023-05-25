Changes by Version
==================
Release Notes.

8.16.0
------------------

* Exclude `synthetic` methods for the WitnessMethod mechanism
* Support ForkJoinPool trace
* Support clickhouse-jdbc-plugin trace sql parameters
* Support monitor jetty server work thread pool metric
* Support Jersey REST framework
* Fix ClassCastException when SQLServer inserts data 
* [Chore] Exclude org.checkerframework:checker-qual and com.google.j2objc:j2objc-annotations
* [Chore] Exclude proto files in the generated jar
* Fix Jedis-2.x plugin can not get host info in jedis 3.3.x+
* Change the classloader to locate the agent path in AgentPackagePath, from `SystemClassLoader` to AgentPackagePath's loader.
* Support Grizzly Trace
* Fix possible IllegalStateException when using Micrometer.
* Support to config custom decryption methods for kafka configurations and fix bug `kafka-reporter-plugin-8.15.0 throw exception "java.lang.NoClassDefFoundError: com/google/gson/Gson"`
#### Documentation


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/175?closed=1)

------------------
Find change logs of all versions [here](changes).
