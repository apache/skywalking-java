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

package org.apache.skywalking.apm.agent.core.boot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServiceManagerOrderingTest {

    private final List<String> prepareOrder = new ArrayList<>();
    private final List<String> bootOrder = new ArrayList<>();
    private final List<String> shutdownOrder = new ArrayList<>();

    private class RecordingService implements BootService {
        private final String name;
        private final int priority;

        private RecordingService(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public void prepare() {
            prepareOrder.add(name);
        }

        @Override
        public void boot() {
            bootOrder.add(name);
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void shutdown() {
            shutdownOrder.add(name);
        }

        @Override
        public int priority() {
            return priority;
        }
    }

    @After
    public void tearDown() throws Exception {
        setBootedServices(new LinkedHashMap<>());
    }

    @Test
    public void higherPriorityBootsFirstAndShutsDownLast() throws Exception {
        Map<Class, BootService> services = new LinkedHashMap<>();
        services.put(Integer.class, new RecordingService("low", 0));
        services.put(Long.class, new RecordingService("high", Integer.MAX_VALUE));
        services.put(Short.class, new RecordingService("mid", 100));
        setBootedServices(services);

        invoke("prepare");
        invoke("startup");
        ServiceManager.INSTANCE.shutdown();

        assertThat(prepareOrder, is(Arrays.asList("high", "mid", "low")));
        assertThat(bootOrder, is(Arrays.asList("high", "mid", "low")));
        assertThat(shutdownOrder, is(Arrays.asList("low", "mid", "high")));
    }

    private void setBootedServices(Map<Class, BootService> services) throws Exception {
        Field field = ServiceManager.class.getDeclaredField("bootedServices");
        field.setAccessible(true);
        field.set(ServiceManager.INSTANCE, services);
    }

    private void invoke(String method) throws Exception {
        Method m = ServiceManager.class.getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(ServiceManager.INSTANCE);
    }
}
