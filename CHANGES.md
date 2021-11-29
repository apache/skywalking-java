Changes by Version
==================
Release Notes.

8.9.0
------------------

* Support `Transaction` and fix duplicated methods enhancements for `jedis-2.x` plugin.
* Add ConsumerWrapper/FunctionWrapper to support CompletableFuture.x.thenAcceptAsync/thenApplyAsync.
* Build CLI from Docker instead of source codes, add alpine based Docker image.
* Support set instance properties in json format.
* Upgrade grpc-java to 1.42.1 and protoc to 3.17.3 to allow using native Mac osx-aarch_64 artifacts.

#### Documentation

* Add a FAQ, `Why is `-Djava.ext.dirs` not supported?`.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/99?closed=1)

------------------
Find change logs of all versions [here](changes).
