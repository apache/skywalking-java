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

package org.apache.skywalking.apm.plugin.pulsar.v28x;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import org.apache.pulsar.client.impl.LookupService;
import org.apache.pulsar.client.impl.MessageImpl;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.pulsar.common.api.proto.MessageMetadata;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.plugin.pulsar.common.MessagePropertiesInjector;
import org.apache.skywalking.apm.plugin.pulsar.common.ProducerEnhanceRequiredInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProducerConstructorInterceptorTest {

    private static final String SERVICE_URL = "pulsar://localhost:6650";
    private static final String TOPIC_NAME = "persistent://my-tenant/my-ns/my-topic";
    private static final String HEAD_KEY = "testKey";
    private static final String HEAD_VALUE = "testValue";

    @Mock
    private PulsarClientImpl pulsarClient;
    @Mock
    private LookupService lookupService;
    @Mock
    private CarrierItem carrierItem;
    private MessageImpl<?> message;

    private ProducerConstructorInterceptor constructorInterceptor;

    private EnhancedInstance enhancedInstance = new EnhancedInstance() {

        private ProducerEnhanceRequiredInfo requiredInfo;

        @Override
        public Object getSkyWalkingDynamicField() {
            return requiredInfo;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.requiredInfo = (ProducerEnhanceRequiredInfo) value;
        }
    };

    @Before
    public void setUp() {
        when(lookupService.getServiceUrl()).thenReturn(SERVICE_URL);
        when(pulsarClient.getLookup()).thenReturn(lookupService);
        when(carrierItem.getHeadKey()).thenReturn(HEAD_KEY);
        when(carrierItem.getHeadValue()).thenReturn(HEAD_VALUE);
        constructorInterceptor = new ProducerConstructorInterceptor();
        message = MessageImpl.create(new MessageMetadata(), ByteBuffer.allocate(1), null);
    }

    @Test
    public void testOnConsumer() {
        constructorInterceptor.onConstruct(enhancedInstance, new Object[] {
                pulsarClient,
                TOPIC_NAME
        });
        ProducerEnhanceRequiredInfo requiredInfo = (ProducerEnhanceRequiredInfo) enhancedInstance.getSkyWalkingDynamicField();
        assertThat(requiredInfo.getServiceUrl(), is(SERVICE_URL));
        assertThat(requiredInfo.getTopic(), is(TOPIC_NAME));
        final MessagePropertiesInjector propertiesInjector = requiredInfo.getPropertiesInjector();
        Assert.assertNotNull(propertiesInjector);
        propertiesInjector.inject(message, carrierItem);
        assertThat(message.getProperty(HEAD_KEY), is(HEAD_VALUE));
    }
}
