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

package org.apache.skywalking.apm.plugin.websphereliberty.v23;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.plugin.websphereliberty.v23.async.AsyncType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RunnableTest {

    private CompleteRunnableInterceptor completeRunnableInterceptor;
    private DispatchRunnableInterceptor dispatchRunnableInterceptor;

    @Mock
    private EnhancedInstance completeEnhancedInstance;
    @Mock
    private EnhancedInstance dispatchEnhancedInstance;

    @Before
    public void setUp() throws Exception {
        completeRunnableInterceptor = new CompleteRunnableInterceptor();
        dispatchRunnableInterceptor = new DispatchRunnableInterceptor();

        when(completeEnhancedInstance.getSkyWalkingDynamicField()).thenReturn(AsyncType.COMPLETE);
        when(dispatchEnhancedInstance.getSkyWalkingDynamicField()).thenReturn(AsyncType.DISPATCH);
    }

    @Test
    public void testCompleteRunnableConstruct() throws Throwable {
        completeRunnableInterceptor.onConstruct(completeEnhancedInstance, null);
        Assert.assertEquals(completeEnhancedInstance.getSkyWalkingDynamicField(), AsyncType.COMPLETE);
    }

    @Test
    public void testDispatchRunnableConstruct() throws Throwable {
        dispatchRunnableInterceptor.onConstruct(dispatchEnhancedInstance, null);
        Assert.assertEquals(dispatchEnhancedInstance.getSkyWalkingDynamicField(), AsyncType.DISPATCH);
    }
}