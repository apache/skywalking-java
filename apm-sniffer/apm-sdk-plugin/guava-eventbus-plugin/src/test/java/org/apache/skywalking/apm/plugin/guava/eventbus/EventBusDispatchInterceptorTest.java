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
import static org.junit.Assert.assertNull;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

@RunWith(MockitoJUnitRunner.class)
public class EventBusDispatchInterceptorTest {

    private EventBusDispatchInterceptor interceptor;
    private Object originalEventObj;
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ContextSnapshot contextSnapshot;

    private MockedStatic<ContextManager> mockedContextManager;

    @Before
    public void setUp() throws Exception {
        interceptor = new EventBusDispatchInterceptor();
        originalEventObj = new Object();

        mockedContextManager = Mockito.mockStatic(ContextManager.class);
        mockedContextManager.when(ContextManager::capture).thenReturn(contextSnapshot);
    }

    @After
    public void tearDown() {
        mockedContextManager.close();
    }

    @Test
    public void test() throws Throwable {
        Object[] arguments = new Object[] {originalEventObj};
        interceptor.beforeMethod(null, null, arguments, new Class[1], null);
        assertEquals(EventWrapper.class, arguments[0].getClass());
        assertEquals(originalEventObj, ((EventWrapper) arguments[0]).getEvent());
        assertNotNull(((EventWrapper) arguments[0]).getContextSnapshot());
    }

    @Test
    public void testNullEventObject() throws Throwable {
        Object[] arguments = new Object[1];
        interceptor.beforeMethod(null, null, arguments, new Class[1], null);
        assertNull(arguments[0]);
    }

}
