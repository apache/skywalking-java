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

package org.apache.skywalking.apm.toolkit.activation.trace;

import com.google.gson.Gson;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.os.OSUtil;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.toolkit.activation.threadpool.AsyncThreadFactory;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;

public class TraceMsgContextInterceptor implements StaticMethodsAroundInterceptor {

    private static ILog logger = LogManager.getLogger(TraceMsgContextInterceptor.class);

    private static final String OPERATE_NAME_PREFIX = "MsgBusinessConsumer";
    private static final String CONSUMER_TRACE_ID = "ConsumerTraceID";
    private static final String CONSUMER_POD_IP = "ConsumerPodIp";
    private static final Gson GSON = new Gson();
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 4;
    private static final int THREAD_POOL_QUEUE_SIZE = 10000;
    private static ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(THREAD_POOL_QUEUE_SIZE);
    private static final ExecutorService executor = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE,
            60, TimeUnit.SECONDS, blockingQueue,
            new AsyncThreadFactory("TraceMsgContextPool"), new CustomRejectedExecutionHandler());

    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.warn("TraceMsgContextInterceptor AsyncTrace thread pool is full, rejecting the task");
        }
    }

    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                             MethodInterceptResult result) {
    }

    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                              Object ret) {
        String currentTraceId = TraceContext.traceId();
        executor.execute(() -> {
            String context = (String) allArguments[0];
            Throwable throwable = null;
            if (allArguments.length > 1) {
                throwable = (Throwable) allArguments[1];
            }
            Map<String, String> values = GSON.fromJson(context, Map.class);
            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                String headKey = next.getHeadKey();
                next.setHeadValue(values.get(headKey));
            }
            if (contextCarrier.isValid()) {
                AbstractSpan activeSpan = ContextManager.createEntrySpan(OPERATE_NAME_PREFIX,
                        contextCarrier);
                if (throwable != null) {
                    activeSpan.errorOccurred();
                    activeSpan.log(throwable);
                }
                activeSpan.tag(Tags.ofKey(CONSUMER_TRACE_ID), currentTraceId);
                activeSpan.tag(Tags.ofKey(CONSUMER_POD_IP), OSUtil.getIPV4());
                activeSpan.setComponent(ComponentsDefine.MSG_BUSINESS_CONSUMER);
                SpanLayer.asMQ(activeSpan);
                ContextManager.stopSpan();
            }
        });
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                                      Throwable t) {
        logger.error("Failed to getDefault trace Id.", t);
    }
}
