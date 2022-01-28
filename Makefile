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
CLI_VERSION ?= 0.9.0 # CLI version inside agent image should always use an Apache released artifact.

.PHONY: build
build:
	./mvnw --batch-mode clean package -Dmaven.test.skip=true

.PHONY: dist
dist: build
	tar czf apache-skywalking-java-agent-$(TAG).tgz $(AGENT_PACKAGE)
	gpg --armor --detach-sig apache-skywalking-java-agent-$(TAG).tgz
	shasum -a 512 apache-skywalking-java-agent-$(TAG).tgz > apache-skywalking-java-agent-$(TAG).tgz.sha512

# Docker build

base.all := alpine java8 java11 java17
base.each = $(word 1, $@)

base.image.alpine := alpine:3
base.image.java8 := eclipse-temurin:8-jre
base.image.java11 := eclipse-temurin:11-jre
base.image.java17 := eclipse-temurin:17-jre

docker.%: PLATFORMS =
docker.%: LOAD_OR_PUSH = --load
docker.push.%: PLATFORMS = --platform linux/amd64,linux/arm64
docker.push.%: LOAD_OR_PUSH = --push

.PHONY: $(base.all)
$(base.all:%=docker.%): BASE_IMAGE=$($(base.each:docker.%=base.image.%))
$(base.all:%=docker.%): FINAL_TAG=$(TAG)-$(base.each:docker.%=%)
$(base.all:%=docker.push.%): BASE_IMAGE=$($(base.each:docker.push.%=base.image.%))
$(base.all:%=docker.push.%): FINAL_TAG=$(TAG)-$(base.each:docker.push.%=%)
$(base.all:%=docker.%) $(base.all:%=docker.push.%): skywalking-agent
	docker buildx create --use --driver docker-container --name skywalking_main > /dev/null 2>&1 || true
	docker buildx build $(PLATFORMS) $(LOAD_OR_PUSH) \
        --no-cache \
        --build-arg BASE_IMAGE=$(BASE_IMAGE) \
        --build-arg DIST=$(AGENT_PACKAGE) \
        --build-arg SKYWALKING_CLI_VERSION=$(CLI_VERSION) \
        . -t $(HUB)/$(NAME):$(FINAL_TAG)
	docker buildx rm skywalking_main || true

.PHONY: docker docker.push
docker: $(base.all:%=docker.%)
docker.push: $(base.all:%=docker.push.%)
