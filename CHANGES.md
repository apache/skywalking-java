Changes by Version
==================
Release Notes.

9.5.0
------------------

* Add the virtual thread executor plugin
* Fix Conflicts apm-jdk-threadpool-plugin conflicts with apm-jdk-forkjoinpool-plugin
* Fix NPE in hikaricp-plugin if JDBC URL is not set
* Agent kernel services could be not-booted-yet as ServiceManager#INSTANCE#boot executed after agent transfer
  initialization. Delay so11y metrics#build when the services are not ready to avoid MeterService status is not
  initialized.
* Fix retransform failure when enhancing both parent and child classes.
* Add support for `dameng(DM)` jdbc url format in `URLParser`.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/236?closed=1)

------------------
Find change logs of all versions [here](changes).
