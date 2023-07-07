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

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AerospikeClientMethodInterceptor implements InstanceMethodsAroundInterceptor {
    private static final Set<String> OPERATION_MAPPING_READ = new HashSet<>(Arrays.asList(
            "get",
            "prepend",
            "exists",
            "getHeader",
            "scanAll",
            "scanNode",
            "scanPartitions",
            "getLargeList",
            "getLargeMap",
            "getLargeSet",
            "getLargeStack",
            "query",
            "queryNode",
            "queryPartitions",
            "queryAggregate",
            "queryAggregateNode",
            "info"
    ));

    private static final Set<String> OPERATION_MAPPING_WRITE = new HashSet<>(Arrays.asList(
            "append",
            "put",
            "add",
            "delete",
            "touch",
            "operate",
            "register",
            "registerUdfString",
            "removeUdf",
            "execute"
    ));

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        String methodName = method.getName();
        AbstractSpan span = ContextManager.createExitSpan("Aerospike/" + methodName, peer);
        span.setComponent(ComponentsDefine.AEROSPIKE);
        Tags.CACHE_TYPE.set(span, "Aerospike");
        SpanLayer.asCache(span);
        parseOperation(methodName).ifPresent(op -> Tags.CACHE_OP.set(span, op));
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.log(t);
    }

    private Optional<String> parseOperation(String cmd) {
        if (OPERATION_MAPPING_READ.contains(cmd)) {
            return Optional.of("read");
        }
        if (OPERATION_MAPPING_WRITE.contains(cmd)) {
            return Optional.of("write");
        }
        return Optional.empty();
    }
}