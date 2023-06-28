/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.skywalking.apm.plugin.aerospike;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.named;

public enum AerospikeMethodMatch {
    INSTANCE;

    public ElementMatcher<MethodDescription> getAerospikeMethodMatcher() {
        return named("append")
                .or(named("put"))
                .or(named("prepend"))
                .or(named("add"))
                .or(named("delete"))
                .or(named("touch"))
                .or(named("exists"))
                .or(named("get"))
                .or(named("getHeader"))
                .or(named("operate"))
                .or(named("scanAll"))
                .or(named("scanNode"))
                .or(named("scanPartitions"))
                .or(named("getLargeList"))
                .or(named("getLargeMap"))
                .or(named("getLargeSet"))
                .or(named("getLargeStack"))
                .or(named("register"))
                .or(named("registerUdfString"))
                .or(named("removeUdf"))
                .or(named("execute"))
                .or(named("query"))
                .or(named("queryNode"))
                .or(named("queryPartitions"))
                .or(named("queryAggregate"))
                .or(named("queryAggregateNode"))
                .or(named("info"));
    }
}