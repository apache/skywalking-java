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

package org.apache.skywalking.apm.testcase.vertxcore.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.skywalking.apm.testcase.vertxcore.util.CustomMessage;
import org.apache.skywalking.apm.testcase.vertxcore.util.CustomMessageCodec;

public class VertxCoreController extends AbstractVerticle {

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.get("/vertx-core-4-scenario/case/core-case").handler(this::handleCoreCase);
        router.get("/vertx-core-4-scenario/case/executeTest").handler(this::executeTest);
        router.head("/vertx-core-4-scenario/case/healthCheck").handler(this::healthCheck);
        vertx.createHttpServer().requestHandler(router).listen(8080);

        vertx.eventBus().registerDefaultCodec(CustomMessage.class, new CustomMessageCodec());
        vertx.deployVerticle(LocalReceiver.class.getName());
    }

    private void handleCoreCase(RoutingContext routingContext) {
        vertx.createHttpClient().request(HttpMethod.GET, 8080, "localhost",
                "/vertx-core-4-scenario/case/executeTest").onComplete(it -> {
                    if (it.succeeded()) {
                        it.result().end();
                        it.result().response().onComplete(it2 -> {
                            if (it2.succeeded()) {
                                routingContext.response().setStatusCode(it2.result().statusCode()).end();
                            }
                        });
                    }
                });
    }

    private void executeTest(RoutingContext routingContext) {
        Promise<Void> localMessageFuture = Promise.promise();
        CustomMessage localMessage = new CustomMessage("local-message-receiver request");
        vertx.eventBus().request("local-message-receiver", localMessage, reply -> {
            if (reply.succeeded()) {
                CustomMessage replyMessage = (CustomMessage) reply.result().body();
                replyMessage.getMessage();
                localMessageFuture.complete();
            } else {
                localMessageFuture.fail(reply.cause());
            }
        });

        Promise<Void> clusterMessageFuture = Promise.promise();
        CustomMessage clusterWideMessage = new CustomMessage("cluster-message-receiver request");
        vertx.eventBus().request("cluster-message-receiver", clusterWideMessage, reply -> {
            if (reply.succeeded()) {
                CustomMessage replyMessage = (CustomMessage) reply.result().body();
                replyMessage.getMessage();
                clusterMessageFuture.complete();
            } else {
                clusterMessageFuture.fail(reply.cause());
            }
        });

        localMessageFuture.future().onComplete(localHandler -> {
            if (localHandler.succeeded()) {
                clusterMessageFuture.future().onComplete(clusterHandler -> {
                    if (clusterHandler.succeeded()) {
                        routingContext.response().setStatusCode(200).end();
                    } else {
                        routingContext.response().setStatusCode(500).end(Json.encodePrettily(clusterHandler.cause()));
                    }
                });
            } else {
                routingContext.response().setStatusCode(500).end(Json.encodePrettily(localHandler.cause()));
            }
        });
    }

    private void healthCheck(RoutingContext routingContext) {
        routingContext.response().setStatusCode(200).end("Success");
    }
}
