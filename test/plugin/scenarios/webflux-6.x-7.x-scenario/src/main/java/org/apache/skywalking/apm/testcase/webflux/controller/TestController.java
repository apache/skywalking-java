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

package org.apache.skywalking.apm.testcase.webflux.controller;

import org.apache.skywalking.apm.toolkit.webflux.v6.WebFluxSkyWalkingOperators;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@RestController
@RequestMapping("/case")
public class TestController {

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Entry case. Calls {@link #receive()} over HTTP so the trace crosses a real request boundary:
     * the WebClient plugin writes the sw8 header on the exit span, and the WebFlux DispatcherHandler
     * interceptor has to read it back to build the entry span of the downstream segment. That
     * downstream segment's cross-process ref is what proves header extraction works.
     */
    @GetMapping("/webflux")
    public Mono<String> webflux() {
        return WebClient.create()
            .get()
            .uri("http://localhost:" + serverPort + "/case/receive")
            .retrieve()
            .bodyToMono(String.class);
    }

    /**
     * Downstream endpoint. Also drives the webflux toolkit through its Reactor 3.5+ package, so the
     * toolkit activation is covered by the same request.
     */
    @GetMapping("/receive")
    public Mono<String> receive() {
        return Mono.deferContextual(ctx -> WebFluxSkyWalkingOperators.continueTracing(
            Context.of(ctx), () -> Mono.just("Success")));
    }

    @RequestMapping("/healthCheck")
    public Mono<String> healthCheck() {
        return Mono.just("healthCheck");
    }
}
