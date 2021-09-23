# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

SHELL := /bin/bash -o pipefail

HUB ?= skywalking
NAME ?= skywalking-java
TAG ?= latest
AGENT_PACKAGE ?= skywalking-agent

.PHONY: build
build:
	./mvnw --batch-mode clean package -Dmaven.test.skip=true

.PHONY: dist
dist: build
	tar czf apache-skywalking-java-agent-$(TAG).tgz $(AGENT_PACKAGE)
	gpg --armor --detach-sig apache-skywalking-java-agent-$(TAG).tgz
	shasum -a 512 apache-skywalking-java-agent-$(TAG).tgz > apache-skywalking-java-agent-$(TAG).tgz.sha512

# Docker build

JAVA_VERSIONS := 8 11 12 13 14 15 16
JAVA_VERSION = $(word 1, $@)

.PHONY: $(JAVA_VERSIONS:%=java%)
$(JAVA_VERSIONS:%=docker.java%): skywalking-agent
	docker build --no-cache --build-arg BASE_IMAGE=adoptopenjdk/openjdk$(JAVA_VERSION:docker.java%=%):alpine-jre --build-arg DIST=$(AGENT_PACKAGE) . -t $(HUB)/$(NAME):$(TAG)-$(JAVA_VERSION:docker.%=%)

.PHONY: docker
docker: $(JAVA_VERSIONS:%=docker.java%)

# Docker push

.PHONY: $(JAVA_VERSIONS:%=docker.push.java%)
$(JAVA_VERSIONS:%=docker.push.java%): $(JAVA_VERSIONS:%=docker.java%)
	docker push $(HUB)/$(NAME):$(TAG)-$(JAVA_VERSION:docker.push.%=%)

.PHONY: docker.push
docker.push: $(JAVA_VERSIONS:%=docker.java%)
