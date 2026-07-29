/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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

package org.apache.skywalking.apm.plugin.jdk.threading;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

/**
 * Interceptor for ThreadPoolExecutor.invokeAll() and invokeAny() methods.
 *
 * <p>These methods wrap the original Callable tasks in RunnableFuture, which causes
 * the tracing context to be lost. This interceptor captures the active context before
 * the method call and wraps each Callable to restore the context when executed.</p>
 */
public class ThreadPoolExecutorInvokeInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(final EnhancedInstance objInst, final Method method, final Object[] allArguments,
        final Class<?>[] argumentsTypes, final MethodInterceptResult result) {

        if (allArguments.length == 0) {
            return;
        }

        // Get the Callable collection (first argument)
        Object arg = allArguments[0];
        if (!(arg instanceof Collection)) {
            return;
        }

        Collection<?> callables = (Collection<?>) arg;
        if (callables.isEmpty()) {
            return;
        }

        // Capture the current tracing context
        ContextSnapshot snapshot = ContextManager.capture();

        // Wrap each Callable to restore the tracing context
        List<Callable<?>> wrappedCallables = new ArrayList<>(callables.size());
        for (Object callable : callables) {
            if (callable instanceof Callable) {
                wrappedCallables.add(new ContextRestoringCallable<>((Callable<?>) callable, snapshot));
            } else {
                wrappedCallables.add((Callable<?>) callable);
            }
        }

        // Replace the original argument with wrapped callables
        allArguments[0] = wrappedCallables;
    }

    @Override
    public Object afterMethod(final EnhancedInstance objInst, final Method method, final Object[] allArguments,
        final Class<?>[] argumentsTypes, final Object ret) {
        return ret;
    }

    @Override
    public void handleMethodException(final EnhancedInstance objInst, final Method method, final Object[] allArguments,
        final Class<?>[] argumentsTypes, final Throwable t) {
        // No special handling needed
    }

    /**
     * A Callable wrapper that restores the tracing context before execution.
     */
    private static class ContextRestoringCallable<T> implements Callable<T> {
        private final Callable<T> delegate;
        private final ContextSnapshot snapshot;

        ContextRestoringCallable(Callable<T> delegate, ContextSnapshot snapshot) {
            this.delegate = delegate;
            this.snapshot = snapshot;
        }

        @Override
        public T call() throws Exception {
            ContextManager.continued(snapshot);
            try {
                return delegate.call();
            } finally {
                ContextManager.stopSpan();
            }
        }
    }
}
