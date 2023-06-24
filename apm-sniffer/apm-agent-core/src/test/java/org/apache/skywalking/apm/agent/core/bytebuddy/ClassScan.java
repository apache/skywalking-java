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

package org.apache.skywalking.apm.agent.core.bytebuddy;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Scan the classes, and notify the listener(s)
 */
public class ClassScan {

    private final List<ListenerCache> listeners;

    public ClassScan() {
        this.listeners = new LinkedList<>();
    }

    /**
     * Register the callback listener
     *
     * @param listener to be called after class found
     */
    public void registerListener(ClassScanListener listener) {
        listeners.add(new ListenerCache(listener));
    }

    /**
     * Begin to scan classes.
     */
    public void scan() throws Exception {
        ClassPath classpath = ClassPath.from(this.getClass().getClassLoader());
        ImmutableSet<ClassPath.ClassInfo> classes = classpath.getAllClasses();
        String packagePrefix = "org.apache.skywalking.";
        for (ClassPath.ClassInfo classInfo : classes) {
            if (!classInfo.getName().startsWith(packagePrefix)) {
                continue;
            }
            Class<?> aClass = classInfo.load();

            for (ListenerCache listener : listeners) {
                if (listener.classMatch().matches(TypeDescription.ForLoadedType.of(aClass))) {
                    listener.addMatch(aClass);
                }
            }
        }

        for (ListenerCache listener : listeners) {
            listener.complete();
        }
    }

    public interface ClassScanListener {

        ElementMatcher<TypeDescription> classMatch();

        void notify(Class aClass) throws Exception;
    }

    private class ListenerCache {
        private ClassScanListener listener;
        private List<Class<?>> matchedClass;

        private ListenerCache(ClassScanListener listener) {
            this.listener = listener;
            matchedClass = new LinkedList<>();
        }

        private ElementMatcher<TypeDescription> classMatch() {
            return this.listener.classMatch();
        }

        private void addMatch(Class aClass) {
            matchedClass.add(aClass);
        }

        private void complete() throws Exception {
            matchedClass.sort(Comparator.comparing(Class::getName));
            for (Class<?> aClass : matchedClass) {
                listener.notify(aClass);
            }
        }
    }
}
