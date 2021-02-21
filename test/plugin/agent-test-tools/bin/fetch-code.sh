#!/usr/bin/env bash

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

WORK_DIRECTORY=$1
REPOSITORY=$2
COMMIT_ID=$3
DIST_DIRECTORY=$4

ROOT_DIR="$(cd "$(dirname $0)"; pwd)"

git clone $REPOSITORY $WORK_DIRECTORY

cd $WORK_DIRECTORY

git checkout $COMMIT_ID

"$ROOT_DIR"/../../../../mvnw -B package -DskipTests

[[ -d $DIST_DIRECTORY ]] || mkdir -p $DIST_DIRECTORY

cp $WORK_DIRECTORY/dist/* $DIST_DIRECTORY/
