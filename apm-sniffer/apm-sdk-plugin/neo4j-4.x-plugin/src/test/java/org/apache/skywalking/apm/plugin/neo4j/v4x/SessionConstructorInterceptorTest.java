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

package org.apache.skywalking.apm.plugin.neo4j.v4x;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.MockContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
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
import org.neo4j.driver.internal.DatabaseName;

@RunWith(MockitoJUnitRunner.class)
public class SessionConstructorInterceptorTest {

    private SessionConstructorInterceptor interceptor;
    private EnhancedInstance enhancedInstance;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private DatabaseName databaseName;

    private MockedStatic<ContextManager> mockedContextManager;

    @Before
    public void setUp() throws Exception {
        interceptor = new SessionConstructorInterceptor();
        enhancedInstance = new EnhancedInstance() {
            private Object value;

            @Override
            public Object getSkyWalkingDynamicField() {
                return value;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.value = value;
            }
        };
        mockedContextManager = Mockito.mockStatic(ContextManager.class);
        mockedContextManager.when(ContextManager::capture).thenReturn(MockContextSnapshot.INSTANCE.mockContextSnapshot());
    }

    @After
    public void tearDown() {
        mockedContextManager.close();
    }

    @Test
    public void testWithDatabaseName() throws Throwable {
        interceptor.onConstruct(enhancedInstance, new Object[]{null, null, databaseName});
        assertNotNull(enhancedInstance.getSkyWalkingDynamicField());
        SessionRequiredInfo requiredInfo = (SessionRequiredInfo) enhancedInstance.getSkyWalkingDynamicField();
        assertThat(requiredInfo.getContextSnapshot(), is(ContextManager.capture()));
    }
}
