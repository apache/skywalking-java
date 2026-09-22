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

public class HttpAsyncClientDoExecuteInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            MethodInterceptResult result) throws Throwable {
        if (!ContextManager.isActive()) {
            return;
        }

        AsyncExitSpan exitSpan = new AsyncExitSpan((HttpHost) allArguments[0]);

        AsyncRequestProducer producer = (AsyncRequestProducer) allArguments[1];
        AsyncResponseConsumer consumer = (AsyncResponseConsumer) allArguments[2];
        FutureCallback callback = (FutureCallback) allArguments[5];

        allArguments[1] = new AsyncRequestProducerWrapper(producer, exitSpan);
        allArguments[2] = new AsyncResponseConsumerWrapper(consumer, exitSpan);
        allArguments[5] = new FutureCallbackWrapper(callback, exitSpan);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            Object ret) throws Throwable {
        if (allArguments[1] instanceof AsyncRequestProducerWrapper) {
            ((AsyncRequestProducerWrapper) allArguments[1]).getExitSpan().callerReturned();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Throwable t) {
        if (allArguments[1] instanceof AsyncRequestProducerWrapper) {
            ((AsyncRequestProducerWrapper) allArguments[1]).getExitSpan().fail(t);
        }
    }
}
