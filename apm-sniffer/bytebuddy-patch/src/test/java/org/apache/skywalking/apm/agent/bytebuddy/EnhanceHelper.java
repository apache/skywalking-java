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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnhanceHelper {

    private static final List<String> INTERCEPTORS = new ArrayList<>();
    private static final List<Map.Entry<String, Throwable>> ERRORS = new ArrayList<>();

    public static void onError(String message, Throwable error) {
        ERRORS.add(new MapEntry<>(message, error));
    }

    public static void addInterceptor(String interceptor) {
        INTERCEPTORS.add(interceptor);
    }

    public static List<String> getInterceptors() {
        return INTERCEPTORS;
    }

    public static List<Map.Entry<String, Throwable>> getErrors() {
        return ERRORS;
    }

    public static void clear() {
        ERRORS.clear();
        INTERCEPTORS.clear();
    }

    private static class MapEntry<T, P> implements Map.Entry<T, P> {
        private final T key;
        private P value;

        public MapEntry(T key, P value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public T getKey() {
            return key;
        }

        @Override
        public P getValue() {
            return value;
        }

        @Override
        public P setValue(P value) {
            this.value = value;
            return value;
        }
    }
}
