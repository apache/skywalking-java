#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Check that plugin pom.xml files do not set maven.compiler.release,
# maven.compiler.source, or maven.compiler.target unless they are on
# the allowlist.  A provided-scope dependency targeting a higher JDK
# does NOT justify raising the compiler level — only plugin source code
# that uses JDK 9+ language features does.

WORK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd ../.. && pwd)"

# Plugins whose source code genuinely uses higher-JDK language features or APIs,
# or that set <maven.compiler.release /> to cancel a parent POM's value.
ALLOWLIST=(
    "bootstrap-plugins/jdk-httpclient-plugin"
    "bootstrap-plugins/jdk-http-plugin"
    "spring-plugins/spring-ai-1.x-plugin"
)

EXIT_CODE=0

while IFS= read -r pom; do
    # Check if this pom is on the allowlist
    allowed=false
    for entry in "${ALLOWLIST[@]}"; do
        if [[ "${pom}" == *"${entry}"* ]]; then
            allowed=true
            break
        fi
    done

    if [ "${allowed}" = true ]; then
        continue
    fi

    # Report the violation
    echo "ERROR: ${pom} sets a compiler level override but is not on the allowlist."
    echo "       If the plugin source code uses JDK 9+ language features, add it to the"
    echo "       allowlist in tools/plugin/check-compiler-overrides.sh."
    echo "       Otherwise, remove the compiler override from the pom.xml."
    echo ""
    EXIT_CODE=1
done < <(grep -rl "maven\.compiler\.\(release\|source\|target\)" \
    "${WORK_DIR}/apm-sniffer/apm-sdk-plugin" \
    "${WORK_DIR}/apm-sniffer/bootstrap-plugins" \
    "${WORK_DIR}/apm-sniffer/optional-plugins" \
    "${WORK_DIR}/apm-sniffer/optional-reporter-plugins" \
    --include="pom.xml")

if [ ${EXIT_CODE} -eq 0 ]; then
    echo "Compiler override check passed."
fi

exit ${EXIT_CODE}
