#!/bin/bash
#
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

home="$(cd "$(dirname $0)"; pwd)"

java -jar ${agent_opts}  "-Dskywalking.agent.service_name=micronaut-scenario-8081" "-Dmicronaut.server.port=8081"  "-Dskywalking.plugin.micronauthttpclient.collect_http_params=true" "-Dskywalking.plugin.micronauthttpserver.collect_http_params=true"  ${home}/../libs/micronaut-http-scenario.jar &
java -jar ${agent_opts} "-Dskywalking.agent.service_name=micronaut-scenario-8080" "-Dmicronaut.server.port=8080"  "-Dskywalking.plugin.micronauthttpclient.collect_http_params=true" "-Dskywalking.plugin.micronauthttpserver.collect_http_params=true"  ${home}/../libs/micronaut-http-scenario.jar &