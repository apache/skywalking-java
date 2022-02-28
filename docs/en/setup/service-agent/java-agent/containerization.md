# Apache SkyWalking Agent Containerized Scenarios

**Docker images are not official ASF releases but provided for convenience. Recommended usage is always to build the
source**

This image only hosts the pre-built SkyWalking Java agent jars, and provides some convenient configurations for
containerized scenarios.

# How to use this image

## Docker

```dockerfile
FROM apache/skywalking-java-agent:8.5.0-jdk8

# ... build your java application
```

You can start your Java application with `CMD` or `ENTRYPOINT`, but you don't need to care about the Java options to
enable SkyWalking agent, it should be adopted automatically.

## Kubernetes

Currently, SkyWalking provides two ways to install the java agent on your services on Kubernetes.

1. To use the java agent more natively, you can try the [java agent injector](https://github.com/apache/skywalking-swck/blob/master/docs/java-agent-injector.md) to inject the java agent image as a sidecar.

2. If you think it's hard to install the injector, you can also use this java agent image as a sidecar as below.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: agent-as-sidecar
spec:
  restartPolicy: Never

  volumes:
    - name: skywalking-agent
      emptyDir: { }

  initContainers:
    - name: agent-container
      image: apache/skywalking-java-agent:8.7.0-alpine
      volumeMounts:
        - name: skywalking-agent
          mountPath: /agent
      command: [ "/bin/sh" ]
      args: [ "-c", "cp -R /skywalking/agent /agent/" ]

  containers:
    - name: app-container
      image: springio/gs-spring-boot-docker
      volumeMounts:
        - name: skywalking-agent
          mountPath: /skywalking
      env:
        - name: JAVA_TOOL_OPTIONS
          value: "-javaagent:/skywalking/agent/skywalking-agent.jar"
```