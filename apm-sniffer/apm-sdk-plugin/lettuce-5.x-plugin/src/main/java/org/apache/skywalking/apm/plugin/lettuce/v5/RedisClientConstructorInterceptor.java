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
 *
 */

package org.apache.skywalking.apm.plugin.lettuce.v5;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.util.StringUtil;

import java.util.stream.Collectors;

public class RedisClientConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        RedisURI redisURI = (RedisURI) allArguments[1];
        RedisClient redisClient = (RedisClient) objInst;
        EnhancedInstance optionsInst = (EnhancedInstance) redisClient.getOptions();
        StringBuilder redisPeer = new StringBuilder();
        if (StringUtil.isNotBlank(redisURI.getSentinelMasterId())) {
            redisPeer.append(redisURI.getSentinelMasterId()).append("[").append(
                    redisURI.getSentinels().stream().map(r -> r.getHost() + ":" + r.getPort())
                            .collect(Collectors.joining(","))).append("]");
        } else {
            redisPeer.append(redisURI.getHost()).append(":").append(redisURI.getPort());
        }
        optionsInst.setSkyWalkingDynamicField(redisPeer.toString());
    }
}
