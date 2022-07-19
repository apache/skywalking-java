# Bootstrap class plugins
All bootstrap plugins are optional, due to unexpected risk. Bootstrap plugins are provided in `bootstrap-plugins` folder.
For using these plugins, you need to put the target plugin jar file into `/plugins`.

Now, we have the following known bootstrap plugins.
* Plugin of JDK HttpURLConnection. Agent is compatible with JDK 1.8+
* Plugin of JDK Callable and Runnable. Agent is compatible with JDK 1.8+
* Plugin of JDK ThreadPoolExecutor. Agent is compatible with JDK 1.8+

### HttpURLConnection Plugin Notice
The plugin of JDK HttpURLConnection depended on `sun.net.*`. When using Java 9+, You should add some JVM options as follows:

| Java version | JVM option                                                                     |
|--------------|--------------------------------------------------------------------------------|
| 9-15         |Nothing to do. Because `--illegal-access` default model is permitted.             |
| 16           |Add `--add-exports java.base/sun.net.www=ALL-UNNAMED` or `--illegal-access=permit` |
| 17+          |Add `--add-exports java.base/sun.net.www=ALL-UNNAMED`                              |

For more information 
1. [JEP 403: Strongly Encapsulate JDK Internals](https://openjdk.org/jeps/403)
2. [A peek into Java 17: Encapsulating the Java runtime internals](https://blogs.oracle.com/javamagazine/post/a-peek-into-java-17-continuing-the-drive-to-encapsulate-the-java-runtime-internals)