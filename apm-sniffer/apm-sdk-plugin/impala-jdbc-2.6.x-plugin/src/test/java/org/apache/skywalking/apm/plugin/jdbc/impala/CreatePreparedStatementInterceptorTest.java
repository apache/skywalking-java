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

package org.apache.skywalking.apm.plugin.jdbc.impala;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.plugin.jdbc.impala.CreatePreparedStatementInterceptor;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreatePreparedStatementInterceptorTest {

    private static final String SQL = "Select * from test";

    private CreatePreparedStatementInterceptor interceptor;

    @Mock
    private EnhancedInstance ret;

    @Mock
    private EnhancedInstance objectInstance;

    @Mock
    private ConnectionInfo connectionInfo;

    @Before
    public void setUp() {
        interceptor = new CreatePreparedStatementInterceptor();

        when(objectInstance.getSkyWalkingDynamicField()).thenReturn(connectionInfo);
    }

    @Test
    public void testResultIsEnhanceInstance() {
        interceptor.afterMethod(objectInstance, null, new Object[]{SQL}, null, ret);
        verify(ret).setSkyWalkingDynamicField(Matchers.any());
    }

    @Test
    public void testResultIsNotEnhanceInstance() {
        interceptor.afterMethod(objectInstance, null, new Object[]{SQL}, null, new Object());
        verify(ret, times(0)).setSkyWalkingDynamicField(Matchers.any());
    }
}
