Apache SkyWalking Java Agent
==========

<img src="http://skywalking.apache.org/assets/logo.svg" alt="Sky Walking logo" height="90px" align="right" />


[![GitHub stars](https://img.shields.io/github/stars/apache/skywalking.svg?style=for-the-badge&label=Stars&logo=github)](https://github.com/apache/skywalking)
[![Twitter Follow](https://img.shields.io/twitter/follow/asfskywalking.svg?style=for-the-badge&label=Follow&logo=twitter)](https://twitter.com/AsfSkyWalking)

[![Maven Central](https://img.shields.io/maven-central/v/org.apache.skywalking/apache-skywalking-apm.svg)](http://skywalking.apache.org/downloads/)
[![CI/IT Tests](https://github.com/apache/skywalking/workflows/CI%20AND%20IT/badge.svg?branch=master)](https://github.com/apache/skywalking/actions?query=workflow%3ACI%2BAND%2BIT+event%3Aschedule+branch%3Amaster)
[![E2E Tests](https://github.com/apache/skywalking/workflows/E2E/badge.svg?branch=master)](https://github.com/apache/skywalking/actions?query=branch%3Amaster+event%3Aschedule+workflow%3AE2E)

SkyWalking-Java: The Java Agent for Apache SkyWalking, which provides the native tracing/metrics/logging abilities for Python project.

SkyWalking: an APM(application performance monitor) system, especially designed for microservices, cloud native and container-based (Docker, Kubernetes, Mesos) architectures.

# Documentation
- [Official documentation](https://skywalking.apache.org/docs/#SkyWalking)
- [The paper of STAM](https://wu-sheng.github.io/STAM/), Streaming Topology Analysis Method.
- [Blog](https://skywalking.apache.org/blog/2020-04-13-apache-skywalking-profiling/) about Use Profiling to Fix the Blind Spot of Distributed Tracing
- [Blog](https://skywalking.apache.org/blog/2020-12-03-obs-service-mesh-with-sw-and-als/) about observing Istio + Envoy service mesh with ALS solution.
- [Blog](https://skywalking.apache.org/blog/obs-service-mesh-vm-with-sw-and-als/) about observing Istio + Envoy service mesh with ALS Metadata-Exchange mechanism (in VMs and / or Kubernetes).

NOTICE, SkyWalking 8.0+ uses [v3 protocols](docs/en/protocols/README.md). They are incompatible with previous releases.

# Downloads
Please head to the [releases page](https://skywalking.apache.org/downloads/) to download a release of Apache SkyWalking.

# Compiling project
Follow this [document](docs/en/guides/How-to-build.md).

# Code of conduct
This project adheres to the Contributor Covenant [code of conduct](https://www.apache.org/foundation/policies/conduct). By participating, you are expected to uphold this code.
Please follow the [REPORTING GUIDELINES](https://www.apache.org/foundation/policies/conduct#reporting-guidelines) to report unacceptable behavior.

# Live Demo
Find the [demo](https://skywalking.apache.org/#demo) and [screenshots](https://skywalking.apache.org/#arch) on our website.


# Contact Us
* Mail list: **dev@skywalking.apache.org**. Mail to `dev-subscribe@skywalking.apache.org`, follow the reply to subscribe the mail list.
* Send `Request to join SkyWalking slack` mail to the mail list(`dev@skywalking.apache.org`), we will invite you in.
* Twitter, [ASFSkyWalking](https://twitter.com/ASFSkyWalking)
* QQ Group: 901167865(Recommended), 392443393
* [bilibili B站 视频](https://space.bilibili.com/390683219)

# License
[Apache 2.0 License.](LICENSE)
