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

package org.apache.skywalking.apm.toolkit.webflux;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Signal;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * WebFlux operators that are capable to reuse tracing context from Reactor's Context.
 */
public final class WebFluxSkyWalkingOperators {

    private WebFluxSkyWalkingOperators() {
        throw new IllegalStateException("You can't instantiate a utility class");
    }

    /**
     * Wraps a runnable with a span.
     *
     * @param signalType - Reactor's signal type
     * @param runnable   - lambda to execute within the tracing context
     * @return consumer of a signal
     */
    public static Consumer<Signal<?>> withSpanInScope(SignalType signalType, Runnable runnable) {
        return signal -> {
            if (signalType != signal.getType()) {
                return;
            }
            withSpanInScope(runnable).accept(signal);
        };
    }

    /**
     * Wraps a runnable with a span.
     *
     * @param signalType - Reactor's signal type
     * @param consumer   - lambda to execute within the tracing context
     * @return consumer of a signal
     */
    public static Consumer<Signal> withSpanInScope(SignalType signalType, Consumer<Signal> consumer) {
        return signal -> {
            if (signalType != signal.getType()) {
                return;
            }
            withSpanInScope(signal.getContext(), () -> consumer.accept(signal));
        };
    }

    /**
     * Wraps a runnable with a span.
     *
     * @param runnable - lambda to execute within the tracing context
     * @return consumer of a signal
     */
    public static Consumer<Signal> withSpanInScope(Runnable runnable) {
        return signal -> {
            Context context = signal.getContext();
            withSpanInScope(context, runnable);
        };
    }

    /**
     * Wraps a runnable with a span.
     *
     * @param context  - Reactor context that contains the tracing context
     * @param runnable - lambda to execute within the tracing context
     */
    public static void withSpanInScope(Context context, Runnable runnable) {
        runnable.run();
    }

    /**
     * Wraps a callable with a span.
     *
     * @param context  - Reactor context that contains the tracing context
     * @param callable - lambda to execute within the tracing context
     * @param <T>      callable's return type
     * @return value from the callable
     */
    public static <T> T withSpanInScope(Context context, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            return sneakyThrow(e);
        } 
    }

    public static <T> T withSpanInScope(ServerWebExchange serverWebExchange, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            return sneakyThrow(e);
        }
    }

    public static void withSpanInScope(ServerWebExchange serverWebExchange, Runnable runnable) {
        runnable.run();
    }

    static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
