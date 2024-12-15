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

package org.apache.skywalking.apm.plugin.spring.patch;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.AdvisedSupport;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class CreateAopProxyInterceptorTest {

    private CreateAopProxyInterceptor interceptor;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Mock
    private AdvisedSupport advisedSupport;

    @Before
    public void setUp() {
        interceptor = new CreateAopProxyInterceptor();

    }

    @Test
    public void testInterceptClassImplementsNoInterfaces() throws Throwable {
        // doReturn(Object.class).when(advisedSupport).getTargetClass();
        // doReturn(Object.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(true, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, true)));
    }

    @Test
    public void testInterceptClassImplementsUserSuppliedInterface() throws Throwable {
        doReturn(MockClassImplementsUserSuppliedInterface.class).when(advisedSupport).getTargetClass();
        doReturn(MockClassImplementsUserSuppliedInterface.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(false, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, false)));
    }

    @Test
    public void testInterceptClassImplementsSpringProxy() throws Throwable {
        // doReturn(MockClassImplementsSpringProxy.class).when(advisedSupport).getTargetClass();
        // doReturn(MockClassImplementsSpringProxy.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(true, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, true)));
    }

    @Test
    public void testInterceptClassImplementsEnhancedInstance() throws Throwable {
        doReturn(MockClassImplementsEnhancedInstance.class).when(advisedSupport).getTargetClass();
        doReturn(MockClassImplementsEnhancedInstance.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(true, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, false)));
    }

    @Test
    public void testClassImplementsEnhancedInstanceAndUserSuppliedInterface() throws Throwable {
        doReturn(MockClassImplementsSpringProxyAndUserSuppliedInterface.class).when(advisedSupport).getTargetClass();
        doReturn(MockClassImplementsSpringProxyAndUserSuppliedInterface.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(false, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, false)));
    }

    @Test
    public void testInterceptClassImplementsSpringProxyAndEnhancedInstance() throws Throwable {
        doReturn(MockClassImplementsSpringProxyAndEnhancedInstance.class).when(advisedSupport).getTargetClass();
        doReturn(MockClassImplementsSpringProxyAndEnhancedInstance.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(true, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, false)));
    }

    @Test
    public void testInterceptClassImplementsSpringProxyAndUserSuppliedInterface() throws Throwable {
        doReturn(MockClassImplementsSpringProxyAndUserSuppliedInterface.class).when(advisedSupport).getTargetClass();
        doReturn(MockClassImplementsSpringProxyAndUserSuppliedInterface.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(false, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, false)));
    }

    @Test
    public void testInterceptClassImplementsEnhancedInstanceAndUserSuppliedInterface() throws Throwable {
        doReturn(MockClassImplementsEnhancedInstanceAndUserSuppliedInterface.class).when(advisedSupport).getTargetClass();
        doReturn(MockClassImplementsEnhancedInstanceAndUserSuppliedInterface.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(false, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, false)));
    }

    @Test
    public void testInterceptClassImplementsSpringProxyAndEnhancedInstanceAndUserSuppliedInterface() throws Throwable {
        doReturn(MockClassImplementsSpringProxyAndEnhancedInstanceAndUserSuppliedInterface.class).when(advisedSupport).getTargetClass();
        doReturn(MockClassImplementsSpringProxyAndEnhancedInstanceAndUserSuppliedInterface.class.getInterfaces()).when(advisedSupport).getProxiedInterfaces();
        assertThat(false, is(interceptor.afterMethod(enhancedInstance, null, new Object[] {advisedSupport}, new Class[] {Object.class}, false)));
    }

    private class MockClassImplementsEnhancedInstance implements EnhancedInstance {

        @Override
        public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {

        }
    }

    private class MockClassImplementsUserSuppliedInterface implements UserSuppliedInterface {

        @Override
        public void methodOfUserSuppliedInterface() {

        }

    }

    private class MockClassImplementsSpringProxy implements SpringProxy {

    }

    private class MockClassImplementsSpringProxyAndEnhancedInstance  implements EnhancedInstance, SpringProxy {

        @Override
        public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {

        }

    }

    private class MockClassImplementsEnhancedInstanceAndUserSuppliedInterface  implements EnhancedInstance, UserSuppliedInterface {

        @Override
        public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {

        }

        @Override
        public void methodOfUserSuppliedInterface() {
        }

    }

    private class MockClassImplementsSpringProxyAndUserSuppliedInterface  implements SpringProxy, UserSuppliedInterface {

        @Override
        public void methodOfUserSuppliedInterface() {
        }

    }

    private class MockClassImplementsSpringProxyAndEnhancedInstanceAndUserSuppliedInterface  implements EnhancedInstance, SpringProxy, UserSuppliedInterface {

        @Override
        public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {

        }

        @Override
        public void methodOfUserSuppliedInterface() {
        }

    }

    interface UserSuppliedInterface {

         void methodOfUserSuppliedInterface();

    }

}
