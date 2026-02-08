package org.apache.skywalking.apm.testcase.lettuce;

import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class RedisClientConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(@Value("${redis.servers:127.0.0.1:6379}") String address) {

        return RedisClient.create("redis://" + address);
    }
}
