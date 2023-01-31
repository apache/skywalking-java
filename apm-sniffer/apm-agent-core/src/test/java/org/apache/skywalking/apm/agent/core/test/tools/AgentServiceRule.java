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

package org.apache.skywalking.apm.agent.core.test.tools;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.IgnoredTracerContext;
import org.apache.skywalking.apm.agent.core.context.TracingContext;
import org.apache.skywalking.apm.agent.core.context.TracingContextListener;
import org.apache.skywalking.apm.agent.core.context.TracingThreadListener;
import org.junit.rules.ExternalResource;

public class AgentServiceRule extends ExternalResource {

    @Override
    protected void after() {
        super.after();
        reset();
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        reset();

        ServiceManager.INSTANCE.boot();
    }

    private void reset() {
        try {
            Field bootedServices = ServiceManager.class.getDeclaredField("bootedServices");
            bootedServices.setAccessible(true);
            bootedServices.set(ServiceManager.INSTANCE, new HashMap<Class<?>, BootService>());

            Field listeners = TracingContext.ListenerManager.class.getDeclaredField("LISTENERS");
            listeners.setAccessible(true);
            listeners.set(TracingContext.ListenerManager.class,
                new LinkedList<TracingContextListener>());

            listeners = IgnoredTracerContext.ListenerManager.class.getDeclaredField("LISTENERS");
            listeners.setAccessible(true);
            listeners.set(IgnoredTracerContext.ListenerManager.class,
                new LinkedList<TracingContextListener>());

            listeners =
                TracingContext.TracingThreadListenerManager.class.getDeclaredField("LISTENERS");
            listeners.setAccessible(true);
            listeners.set(TracingContext.TracingThreadListenerManager.class, new LinkedList<TracingThreadListener>());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
