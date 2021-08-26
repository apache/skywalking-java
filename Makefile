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

BASE_IMAGE ?= adoptopenjdk/openjdk8:alpine
SKIP_TEST ?= false

.PHONY: build
build:
	./mvnw --batch-mode clean package -Dmaven.test.skip=$(SKIP_TEST)

.PHONY: docker
docker: build
	docker build --no-cache --build-arg BASE_IMAGE=$(BASE_IMAGE) . -t $(HUB)/$(NAME):$(TAG)

.PHONY: docker.push
docker.push: docker
	docker push $(HUB)/$(NAME):$(TAG)
