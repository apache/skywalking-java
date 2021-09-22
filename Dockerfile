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

ARG BASE_IMAGE='adoptopenjdk/openjdk8:alpine-jre'

FROM $BASE_IMAGE as cli

WORKDIR /skywalking

ARG SKYWALKING_CLI_VERSION=0.7.0
ENV SKYWALKING_CLI_TGZ=skywalking-cli-$SKYWALKING_CLI_VERSION-bin.tgz
ENV SKYWALKING_CLI_ASC=${SKYWALKING_CLI_TGZ}.asc
ENV SKYWALKING_CLI_SHA512=${SKYWALKING_CLI_TGZ}.sha512

ENV SKYWALKING_CLI_TGZ_URLS \
        https://www.apache.org/dyn/closer.cgi?action=download&filename=skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_TGZ \
        # if the version is outdated, we might have to pull from the dist/archive :/
	    https://www-us.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_TGZ \
	    https://www.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_TGZ \
	    https://archive.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_TGZ

ENV SKYWALKING_CLI_ASC_URLS \
        https://www.apache.org/dyn/closer.cgi?action=download&filename=skywalking/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_ASC \
        # if the version is outdated, we might have to pull from the dist/archive :/
	    https://www-us.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_ASC \
	    https://www.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_ASC \
	    https://archive.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_ASC

ENV SKYWALKING_CLI_SHA512_URLS \
        https://www.apache.org/dyn/closer.cgi?action=download&filename=skywalking/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_SHA512 \
        # if the version is outdated, we might have to pull from the dist/archive :/
	    https://www-us.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_SHA512 \
	    https://www.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_SHA512 \
	    https://archive.apache.org/dist/skywalking/cli/$SKYWALKING_CLI_VERSION/$SKYWALKING_CLI_SHA512


RUN set -eux; \
	\
	apk add --no-cache --virtual .fetch-deps \
		gnupg \
		ca-certificates \
		openssl \
	; \
	\
	wget --timeout=20 -O KEYS https://downloads.apache.org/skywalking/KEYS; \
	gpg --import KEYS; \
	\
	success=; \
	for url in $SKYWALKING_CLI_TGZ_URLS; do \
		if wget --timeout=20  -O ${SKYWALKING_CLI_TGZ} "$url"; then \
			success=1; \
			break; \
		fi; \
	done; \
	[ -n "$success" ]; \
	\
	success=; \
	for url in $SKYWALKING_CLI_SHA512_URLS; do \
		if wget --timeout=20  -O ${SKYWALKING_CLI_SHA512} "$url"; then \
			success=1; \
			break; \
		fi; \
	done; \
	[ -n "$success" ]; \
	\
	sha512sum -c ${SKYWALKING_CLI_SHA512}; \
	\
	success=; \
	for url in $SKYWALKING_CLI_ASC_URLS; do \
		if wget --timeout=20  -O ${SKYWALKING_CLI_ASC} "$url"; then \
			success=1; \
			break; \
		fi; \
	done; \
	[ -n "$success" ]; \
	\
	gpg --batch --verify ${SKYWALKING_CLI_ASC} ${SKYWALKING_CLI_TGZ}; \
	tar -xvf ${SKYWALKING_CLI_TGZ}; \
    mkdir "bin/"; \
	mv skywalking-cli-${SKYWALKING_CLI_VERSION}-bin/bin/swctl-${SKYWALKING_CLI_VERSION}-linux-amd64 bin/swctl; \
	chmod 755 bin/*; \
	command -v gpgconf && gpgconf --kill all || :; \
	ls -la .;

FROM $BASE_IMAGE

ARG DIST=skywalking-agent

RUN apk add --no-cache openssl

LABEL maintainer="kezhenxu94@apache.org"

ENV JAVA_TOOL_OPTIONS=-javaagent:/skywalking/agent/skywalking-agent.jar

WORKDIR /skywalking

ADD $DIST /skywalking/agent

COPY --from=cli /skywalking/bin/swctl /usr/bin/swctl
