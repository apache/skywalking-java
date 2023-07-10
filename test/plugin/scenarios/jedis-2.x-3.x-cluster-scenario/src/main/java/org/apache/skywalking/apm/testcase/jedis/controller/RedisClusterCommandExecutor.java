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

package org.apache.skywalking.apm.testcase.jedis.controller;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisClusterCommandExecutor implements AutoCloseable {
    private JedisCluster jedisCluster;

    public RedisClusterCommandExecutor(String redisCluster) {
        List<String> hostAndPortString = Arrays.asList(redisCluster.split(","));
        Set<HostAndPort> hostAndPorts = new HashSet<>();
        hostAndPortString.forEach(it -> {
            String[] hostPort = it.split(":");
            hostAndPorts.add(new HostAndPort(hostPort[0], Integer.parseInt(hostPort[1])));
        });
        jedisCluster = new JedisCluster(hostAndPorts);
        jedisCluster.echo("Test");
    }

    public void set(String key, String value) {
        jedisCluster.set(key, value);
    }

    public void get(String key) {
        jedisCluster.get(key);
    }

    public void del(String key) {
        jedisCluster.del(key);
    }

    public void close() throws Exception {
        jedisCluster.close();
    }
}
