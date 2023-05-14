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

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Resolve auxiliary type of first agent in the second agent
 */
public class SWClassFileLocator implements ClassFileLocator {

    private final ForInstrumentation.ClassLoadingDelegate classLoadingDelegate;
    private Instrumentation instrumentation;
    private ClassLoader classLoader;
    private String typeNameTrait = "auxiliary$";
    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    public SWClassFileLocator(Instrumentation instrumentation, ClassLoader classLoader, String typeNameTrait) {
        this.instrumentation = instrumentation;
        this.classLoader = classLoader;
        this.typeNameTrait = typeNameTrait;
        classLoadingDelegate = ForInstrumentation.ClassLoadingDelegate.ForDelegatingClassLoader.of(classLoader);
    }

    @Override
    public Resolution locate(String name) throws IOException {
        if (!name.contains(typeNameTrait)) {
            return new Resolution.Illegal(name);
        }
        // get class binary representation in a clean thread, avoiding nest calling transformer!
        Future<Resolution> future = executorService.submit(() -> getResolution(name));
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Resolution getResolution(String name) {
        ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(classLoader, name);
        try {
            instrumentation.addTransformer(classFileTransformer, true);
            try {
                instrumentation.retransformClasses(new Class[]{classLoadingDelegate.locate(name)});
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

    @Override
    public void close() throws IOException {
    }
}
