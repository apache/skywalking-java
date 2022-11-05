/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.pulsar.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.impl.ConsumerImpl;
import org.apache.pulsar.client.impl.ConsumerInterceptors;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.pulsar.client.impl.conf.ConsumerConfigurationData;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;

public class MockConsumerImpl extends ConsumerImpl implements EnhancedInstance {

    protected MockConsumerImpl(final PulsarClientImpl client,
                               final String topic,
                               final ConsumerConfigurationData conf,
                               final ExecutorService listenerExecutor,
                               final int partitionIndex,
                               final CompletableFuture subscribeFuture,
                               final MessageId startMessageId,
                               final Schema schema,
                               final ConsumerInterceptors interceptors,
                               final long backoffIntervalNanos,
                               final long maxBackoffIntervalNanos) {
        super(client, topic, conf, listenerExecutor, partitionIndex, subscribeFuture, null, startMessageId,
              schema, interceptors, backoffIntervalNanos, maxBackoffIntervalNanos
        );
    }

    @Override
    public Object getSkyWalkingDynamicField() {
        return null;
    }

    @Override
    public void setSkyWalkingDynamicField(final Object value) {

    }
}
