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

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/case")
@PropertySource("classpath:application.properties")
public class LettuceController {

    @Value("${redis.servers:127.0.0.1:6379}")
    private String address;

    @RequestMapping("/lettuce-case")
    @ResponseBody
    public String lettuceCase() {
        RedisClient redisClient = RedisClient.create("redis://" + address);
        StatefulRedisConnection<String, String> connection0 = redisClient.connect();
        RedisCommands<String, String> syncCommand = connection0.sync();
        syncCommand.get("key");

        StatefulRedisConnection<String, String> connection1 = redisClient.connect();
        RedisAsyncCommands<String, String> asyncCommands = connection1.async();
        asyncCommands.setAutoFlushCommands(false);
        List<RedisFuture<?>> futures = new ArrayList<>();
        futures.add(asyncCommands.set("key0", "value0"));
        futures.add(asyncCommands.set("key1", "value1"));
        asyncCommands.flushCommands();
        LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));

        StatefulRedisConnection<String, String> connection2 = redisClient.connect();
        RedisReactiveCommands<String, String> reactiveCommands = connection2.reactive();

        Mono<String> result = reactiveCommands.get("key")
                .then(Flux.concat(
                        reactiveCommands.set("key0", "value0"),
                        reactiveCommands.set("key1", "value1")
                ).then())
                .thenReturn("Success");

        result.block();

        // Commands dispatched as a batch are written to the channel and flushed together.
        AsyncCommand<String, String, String> batchSet0 = setCommand("key2", "value2");
        AsyncCommand<String, String, String> batchSet1 = setCommand("key3", "value3");
        connection0.dispatch(Arrays.asList(batchSet0, batchSet1));
        LettuceFutures.awaitAll(5, TimeUnit.SECONDS, batchSet0, batchSet1);

        // The error reply of the first command makes the span of the batch an error, while the span ends with the
        // reply of the last command. Replies come in order, so the first command is done when the last one is.
        AsyncCommand<String, String, Long> batchIncr = incrCommand("key2");
        AsyncCommand<String, String, String> batchSet2 = setCommand("key4", "value4");
        connection0.dispatch(Arrays.asList(batchIncr, batchSet2));
        if (!LettuceFutures.awaitAll(5, TimeUnit.SECONDS, batchSet2)) {
            throw new IllegalStateException("The batch is not completed");
        }

        // A reactive command is completed without an exception on an error reply, which still makes its span an error.
        reactiveCommands.incr("key2").onErrorResume(e -> Mono.empty()).block();

        connection0.close();
        connection1.close();
        connection2.close();
        redisClient.shutdown();
        return "Success";
    }

    private static AsyncCommand<String, String, String> setCommand(String key, String value) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).addKey(key).addValue(value);
        return new AsyncCommand<>(new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8), args));
    }

    private static AsyncCommand<String, String, Long> incrCommand(String key) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).addKey(key);
        return new AsyncCommand<>(new Command<>(CommandType.INCR, new IntegerOutput<>(StringCodec.UTF8), args));
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        return "healthCheck";
    }
}
