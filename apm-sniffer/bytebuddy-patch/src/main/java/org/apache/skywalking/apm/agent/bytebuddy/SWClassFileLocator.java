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
    private String typeNameTrait = "auxiliary$";
    private BlockingQueue<ResolutionFutureTask> queue = new LinkedBlockingDeque<>();
    private Thread thread;

    public SWClassFileLocator(Instrumentation instrumentation, ClassLoader classLoader) {
        this.instrumentation = instrumentation;
        this.classLoader = classLoader;
        classLoadingDelegate = ForInstrumentation.ClassLoadingDelegate.ForDelegatingClassLoader.of(classLoader);

        // Use thread instead of ExecutorService here, avoiding conflicts with apm-jdk-threadpool-plugin
        thread = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    ResolutionFutureTask task = queue.poll(5, TimeUnit.SECONDS);
                    if (task != null) {
                        Resolution resolution = getResolution(task.getClassName());
                        task.getFuture().complete(resolution);
                    }
                }
            } catch (InterruptedException e) {
                // ignore interrupted error
            } catch (Exception e) {
                LOGGER.error(e, "Resolve bytecode of class failed");
            }
        }, "SWClassFileLocator");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Resolution locate(String name) throws IOException {
        if (!name.contains(typeNameTrait)) {
            return new Resolution.Illegal(name);
        }
        // get class binary representation in a clean thread, avoiding nest calling transformer!
        ResolutionFutureTask futureTask = new ResolutionFutureTask(name);
        queue.offer(futureTask);
        try {
            return futureTask.getFuture().get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Resolution getResolution(String name) {
        ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(classLoader, name);
        try {
            instrumentation.addTransformer(classFileTransformer, true);
            try {
                instrumentation.retransformClasses(new Class[]{locateClass(name)});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
        queue.clear();
        thread.interrupt();
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
