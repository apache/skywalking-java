package org.apache.skywalking.apm.testcase.jedis.controller;

import java.util.Set;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

public class ClusterExecutor implements AutoCloseable {

    private final JedisCluster jedisCluster;

    public ClusterExecutor(Set<HostAndPort> jedisClusterNodes) {
        this.jedisCluster = new JedisCluster(jedisClusterNodes);
    }

    public void exec() {
        jedisCluster.set("x", "1");
        jedisCluster.get("x");
        jedisCluster.del("x");
    }

    public void close() {
        this.jedisCluster.close();
    }
}
