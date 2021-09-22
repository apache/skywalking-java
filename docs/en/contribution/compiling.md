# Compiling project
This document will help you compile and build a project in your maven and set your IDE.

Prepare JDK 8+.

* If you clone codes from https://github.com/apache/skywalking-java
```shell
git clone https://github.com/apache/skywalking-java.git
cd skywalking-java
./mvnw clean package -Pall
```

* If you download source codes tar from https://skywalking.apache.org/downloads/

```shell
./mvnw clean package
```

The agent binary package is generated in `skywalking-agent` folder.

Set **Generated Source Codes**(`grpc-java` and `java` folders in **apm-protocol/apm-network/target/generated-sources/protobuf**)
folders if you are using IntelliJ IDE.
