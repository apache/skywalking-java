package org.apache.skywalking.apm.testcase.lettuce.controller;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/case")
@PropertySource("classpath:application.properties")
public class LettuceReactiveController {

    @Value("${redis.servers:127.0.0.1:6379}")
    private String address;

    @Autowired
    private RedisClient redisClient;

    @GetMapping("/lettuce-case")
    public Mono<String> lettuceCase() {

        return Mono.usingWhen(
                Mono.fromCallable(() -> redisClient.connect()),
                connection -> {
                    RedisReactiveCommands<String, String> cmd = connection.reactive();
                    return cmd.get("key")
                            .then(Flux.concat(
                                    cmd.set("key0", "value0"),
                                    cmd.set("key1", "value1")
                            ).then())
                            .thenReturn("Success");
                },
                connection -> Mono.fromFuture(connection.closeAsync())
        );
    }

    @GetMapping("/healthCheck")
    public Mono<String> healthCheck() {
        return Mono.just("healthCheck");
    }
}
