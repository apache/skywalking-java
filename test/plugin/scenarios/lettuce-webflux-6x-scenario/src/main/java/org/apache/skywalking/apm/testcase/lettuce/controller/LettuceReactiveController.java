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
