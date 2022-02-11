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

package org.apache.skywalking.apm.plugin.guava.eventbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class EventBusSubscriberConstructorInterceptorTest {

    private EventBusSubscriberConstructorInterceptor interceptor;
    private EnhancedInstance enhancedInstance;

    @Before
    public void setUp() throws Exception {
        interceptor = new EventBusSubscriberConstructorInterceptor();
        enhancedInstance = new EnhancedInstance() {
            private Object field;

            @Override
            public Object getSkyWalkingDynamicField() {
                return field;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                field = value;
            }
        };
    }

    @Test
    public void test() throws Throwable {
        final Method method = this.getClass().getDeclaredMethod("test");
        Object[] arguments = new Object[] {null, new Object(), method};
        interceptor.onConstruct(enhancedInstance, arguments);

        final Object dynamicField = enhancedInstance.getSkyWalkingDynamicField();
        assertNotNull(dynamicField);
        assertEquals(SubscriberInfo.class, dynamicField.getClass());
        SubscriberInfo subscriberInfo = (SubscriberInfo) dynamicField;
        assertEquals("test", subscriberInfo.getMethodName());
        assertEquals(Object.class.getName(), subscriberInfo.getClassName());
    }
}