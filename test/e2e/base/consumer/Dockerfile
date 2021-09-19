# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ARG AGENT_TAG=9a05ad4b423a380c8da711e2a2b856ee93bc9422-es6
ARG SW_VERSION=8.6.0

FROM maven:3.8.1-jdk-8 as builder

ADD . /consumer_code

WORKDIR /consumer_code
RUN mvn clean package -B -Dsw.version=8.6.0

FROM ghcr.io/apache/skywalking/agent:${AGENT_TAG}

VOLUME /services

COPY --from=builder /consumer_code/target/e2e-service-consumer.jar /services/

ENV JAVA_OPTS=""

RUN echo 'agent.instance_name=${SW_INSTANCE_NAME:consumer}' >> /skywalking/agent/config/agent.config
RUN echo 'logging.output=${SW_LOGGING_OUTPUT:CONSOLE}' >> /skywalking/agent/config/agent.config

CMD ["sh", "-c", "java $JAVA_OPTS -jar /services/e2e-service-consumer.jar"]