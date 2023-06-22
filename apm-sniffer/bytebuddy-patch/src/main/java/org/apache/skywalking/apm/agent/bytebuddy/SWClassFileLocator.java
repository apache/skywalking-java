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

package org.apache.skywalking.apm.agent.bytebuddy;

import net.bytebuddy.dynamic.ClassFileLocator;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Resolve auxiliary type of first agent in the second agent
 */
public class SWClassFileLocator implements ClassFileLocator {
    private static ILog LOGGER = LogManager.getLogger(SWClassFileLocator.class);

    private final ForInstrumentation.ClassLoadingDelegate classLoadingDelegate;
    private Instrumentation instrumentation;
    private ClassLoader classLoader;
    private String[] typeNameTraits = {"auxiliary$", "ByteBuddy$", "$SW"};
    private BlockingQueue<ResolutionFutureTask> queue = new LinkedBlockingDeque<>();
    private Thread thread;
    private int timeoutSeconds = 2;
    private volatile boolean closed;

    public SWClassFileLocator(Instrumentation instrumentation, ClassLoader classLoader, String[] typeNameTraits) {
        this(instrumentation, classLoader);
        this.typeNameTraits = typeNameTraits;
    }

    public SWClassFileLocator(Instrumentation instrumentation, ClassLoader classLoader) {
        this.instrumentation = instrumentation;
        this.classLoader = classLoader;
        classLoadingDelegate = ForInstrumentation.ClassLoadingDelegate.ForDelegatingClassLoader.of(classLoader);

        // Use thread instead of ExecutorService here, avoiding conflicts with apm-jdk-threadpool-plugin
        thread = new Thread(() -> {
            while (!closed) {
                try {
                    ResolutionFutureTask task = queue.poll(5, TimeUnit.SECONDS);
                    if (task != null) {
                        try {
                            Resolution resolution = getResolution(task.getClassName());
                            task.getFuture().complete(resolution);
                        } catch (Throwable e) {
                            task.getFuture().completeExceptionally(e);
                        }
                    }
                } catch (InterruptedException e) {
                    // ignore interrupted error
                } catch (Throwable e) {
                    LOGGER.error(e, "Resolve bytecode of class failure");
                }
            }
        }, "SWClassFileLocator");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Resolution locate(String name) throws IOException {
        if (!match(name)) {
            return new Resolution.Illegal(name);
        }
        if (closed) {
            throw new IOException("resolve class failure: closed");
        }
        // get class binary representation in a clean thread, avoiding nest calling transformer!
        ResolutionFutureTask futureTask = new ResolutionFutureTask(name);
        queue.offer(futureTask);
        try {
            return futureTask.getFuture().get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IOException("resolve class failure: " + name, e);
        }
    }

    private boolean match(String name) {
        boolean matched = false;
        for (String typeNameTrait : typeNameTraits) {
            if (name.contains(typeNameTrait)) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    private Resolution getResolution(String name) throws Exception {
        ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(name);
        try {
            instrumentation.addTransformer(classFileTransformer, true);
            Class aClass = locateClass(name);
            if (aClass == null) {
                return new Resolution.Illegal(name);
            }
            instrumentation.retransformClasses(new Class[]{aClass});
        } finally {
            instrumentation.removeTransformer(classFileTransformer);
        }

        return classFileTransformer.getBinaryRepresentation() != null ?
                new Resolution.Explicit(classFileTransformer.getBinaryRepresentation()) :
                new Resolution.Illegal(name);
    }

    private Class locateClass(String className) {
        // find class in classloader
        try {
            return classLoadingDelegate.locate(className);
        } catch (ClassNotFoundException e) {
        }

        // find class in instrumentation
        Class[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (int i = 0; i < allLoadedClasses.length; i++) {
            Class aClass = allLoadedClasses[i];
            if (className.equals(aClass.getName())) {
                return aClass;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        queue.clear();
        thread.interrupt();
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    private class ResolutionFutureTask {
        private CompletableFuture<Resolution> future;
        private String className;

        public ResolutionFutureTask(String className) {
            this.className = className;
            future = new CompletableFuture<>();
        }

        public CompletableFuture<Resolution> getFuture() {
            return future;
        }

        public String getClassName() {
            return className;
        }
    }
}
