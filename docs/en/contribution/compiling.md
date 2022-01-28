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

# Building Docker images

After you have [compiled the project](#compiling-project) and have generated the `skywalking-agent` folder, you can
build Docker images. [`make docker`] builds the agent Docker images based on `alpine` image, `java8`, `java11` and `java 17`
images by default. If you want to only build part of the images, add suffix `.alpine` or `.java<x>` to the `make`
target, for example:

- Build Docker images based on alpine, Java 8 and Java 11.
  ```shell
  make docker.alpine docker.java8 docker.java11
  ```

You can also customize the Docker registry and Docker image names by specifying the variable `HUB`, `NAME`.

- Set private Docker registry to `gcr.io/skywalking` and custom name to `sw-agent`.
  ```shell
  make docker.alpine HUB=gcr.io/skywalking NAME=sw-agent
  ```
  This will name the Docker image to `gcr.io/skywalking/sw-agent:latest-alpine`

If you want to push the Docker images, add suffix to the make target `docker.`, for example:

- Build and push images based on alpine, Java 8 and Java 11.
  ```shell
  make docker.push.alpine docker.push.java8 docker.push.java11
  ```
