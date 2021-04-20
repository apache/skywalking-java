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

package org.apache.skywalking.apm.plugin.kafka;

import org.apache.kafka.common.TopicPartition;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)
public class AssignMethodInterceptorTest {

    @Mock
    private AssignMethodInterceptor interceptor;

    private Collection<TopicPartition> partitions = new ArrayList<>();

    private EnhancedInstance enhancedInstance = new EnhancedInstance() {
        ConsumerEnhanceRequiredInfo consumerEnhanceRequiredInfo = new ConsumerEnhanceRequiredInfo();

        @Override
        public Object getSkyWalkingDynamicField() {
            return consumerEnhanceRequiredInfo;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.consumerEnhanceRequiredInfo = (ConsumerEnhanceRequiredInfo) value;
        }
    };

    @Before
    public void setup() {
        partitions.add(new TopicPartition("test", 0));
        partitions.add(new TopicPartition("test-1", 1));
        interceptor = new AssignMethodInterceptor();
    }

    @Test
    public void testOnConsumer() {
        interceptor.beforeMethod(enhancedInstance, null, new Object[] {partitions}, new Class[] {Collection.class}, null);
        ConsumerEnhanceRequiredInfo requiredInfo = (ConsumerEnhanceRequiredInfo) enhancedInstance.getSkyWalkingDynamicField();
        assertThat(requiredInfo.getTopics(), is("test;test-1"));
    }
}