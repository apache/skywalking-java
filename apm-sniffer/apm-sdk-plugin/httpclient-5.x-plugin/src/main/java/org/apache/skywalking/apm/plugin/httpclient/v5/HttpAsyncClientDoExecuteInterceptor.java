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

package org.apache.skywalking.apm.plugin.httpclient.v5;

import java.lang.reflect.Method;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.AsyncRequestProducerWrapper;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.AsyncResponseConsumerWrapper;
import org.apache.skywalking.apm.plugin.httpclient.v5.wrapper.FutureCallbackWrapper;

/**
 * Intercepts the internal {@code doExecute(HttpHost, AsyncRequestProducer, AsyncResponseConsumer, ..., FutureCallback)}
 * overload shared by every async client implementation (Internal*AsyncClient, Minimal*AsyncClient, and the
 * classic-facade adapter), whose argument order/types are identical across HttpClient 5.0 through 5.6.
 *
 * <p>Unlike the previous implementation, this interceptor never stores anything in the {@code HttpContext} and
 * never wraps a callback purely to call a parameterless {@code ContextManager.stopSpan()}. It only:
 * <ol>
 *   <li>creates a per-request {@link AsyncRequestSpans} holder, while the caller's context is still active;</li>
 *   <li>wraps the request producer so the exit span is created on the caller thread, synchronously, the moment the
 *   concrete {@code HttpRequest} becomes available;</li>
 *   <li>wraps the response consumer and future callback so the retained span is finished by reference.</li>
 * </ol>
 * Because span creation no longer depends on the {@code HttpContext}, this also fixes HttpClient 5.4+, where the
 * context argument passed by the classic facade and by {@code execute(SimpleHttpRequest, FutureCallback)} is
 * {@code null}.
 */
public class HttpAsyncClientDoExecuteInterceptor implements InstanceMethodsAroundInterceptor {

    private static final int TARGET_INDEX = 0;
    private static final int REQUEST_PRODUCER_INDEX = 1;
    private static final int RESPONSE_CONSUMER_INDEX = 2;
    private static final int CALLBACK_INDEX = 5;

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        if (!ContextManager.isActive()) {
            return;
        }
        if (!(allArguments[REQUEST_PRODUCER_INDEX] instanceof AsyncRequestProducer)
            || !(allArguments[RESPONSE_CONSUMER_INDEX] instanceof AsyncResponseConsumer)) {
            return;
        }

        final HttpHost target = allArguments[TARGET_INDEX] instanceof HttpHost
            ? (HttpHost) allArguments[TARGET_INDEX] : null;
        final AsyncRequestSpans spans = new AsyncRequestSpans(target);

        allArguments[REQUEST_PRODUCER_INDEX] = new AsyncRequestProducerWrapper(
            (AsyncRequestProducer) allArguments[REQUEST_PRODUCER_INDEX], spans);
        allArguments[RESPONSE_CONSUMER_INDEX] = new AsyncResponseConsumerWrapper<>(
            (AsyncResponseConsumer<?>) allArguments[RESPONSE_CONSUMER_INDEX], spans);
        // Wrap even when the caller passed null: it's the only lifecycle hook that sees cancellation and the
        // synchronous-failure-before-consumer-runs path for callers who supplied no callback of their own.
        allArguments[CALLBACK_INDEX] = new FutureCallbackWrapper<>(
            (FutureCallback<?>) allArguments[CALLBACK_INDEX], spans);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        releaseCreationClaim(allArguments);
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        if (allArguments[REQUEST_PRODUCER_INDEX] instanceof AsyncRequestProducerWrapper) {
            AsyncRequestProducerWrapper wrapper = (AsyncRequestProducerWrapper) allArguments[REQUEST_PRODUCER_INDEX];
            wrapper.getSpans().fail(t);
        }
        releaseCreationClaim(allArguments);
    }

    /**
     * Once {@code doExecute} has returned (or thrown), no thread other than a genuinely deferred custom producer
     * has any business claiming span creation — clearing this here keeps {@link AsyncRequestSpans#claimCreation()}
     * honest even if the same thread somehow re-enters.
     */
    private void releaseCreationClaim(Object[] allArguments) {
        if (allArguments[REQUEST_PRODUCER_INDEX] instanceof AsyncRequestProducerWrapper) {
            ((AsyncRequestProducerWrapper) allArguments[REQUEST_PRODUCER_INDEX]).getSpans().callerReturned();
        }
    }
}
