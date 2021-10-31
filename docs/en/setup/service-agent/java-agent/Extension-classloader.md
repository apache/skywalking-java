## Extension ClassLoader
**Extension ClassLoader**: The Extension ClassLoader is a child of Bootstrap ClassLoader and loads the extensions of core java classes from the respective JDK Extension library.
It loads files from jre/lib/ext directory or any other directory pointed by the system property java.ext.dirs.
User decides which parts of the service should be loaded by the extension classloader, so, we open `plugin.plugins_in_ext_class_loader=${SW_PLUGINS_IN_EXT_CLASS_LOADER:}` in the `agent.config` file.

Through this setting, users declare plugins are for excClassloader. Multiple values should be separated by ",".
Also support wildcard(`\*`),like `ehcache\*`. All plugin names are defined in [Agent plugin list](Plugin-list.md)
