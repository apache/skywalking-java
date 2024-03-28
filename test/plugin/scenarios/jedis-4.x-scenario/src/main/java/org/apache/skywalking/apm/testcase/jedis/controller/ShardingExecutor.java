package org.apache.skywalking.apm.testcase.jedis.controller;

import java.util.List;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisSharding;

public class ShardingExecutor implements AutoCloseable {
    private JedisSharding jedisSharding;

    public ShardingExecutor(List<HostAndPort> hostAndPorts) {
        this.jedisSharding = new JedisSharding(hostAndPorts);
    }

    public void exec() {
        jedisSharding.set("x", "1");
        jedisSharding.get("x");
        jedisSharding.del("x");
    }

    public void close() {
        jedisSharding.close();
    }

}
