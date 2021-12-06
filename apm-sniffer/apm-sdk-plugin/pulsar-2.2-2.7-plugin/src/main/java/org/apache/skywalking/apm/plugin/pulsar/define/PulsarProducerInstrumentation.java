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

package org.apache.skywalking.apm.plugin.pulsar.define;

import org.apache.skywalking.apm.plugin.pulsar.common.define.BasePulsarProducerInstrumentation;

/**
 * Pulsar producer instrumentation.
 * <p>
 * The pulsar producer instrumentation use {@link org.apache.pulsar.client.impl.ProducerImpl} as an enhanced class.
 * {@link org.apache.pulsar.client.api.Producer} is a user-oriented interface and the implementations of the Producer
 * are {@link org.apache.pulsar.client.impl.PartitionedProducerImpl} and {@link org.apache.pulsar.client.impl.ProducerImpl}.
 * <p>
 * And the PartitionedProducerImpl is a complex type with multiple ProducerImpl to support uses send messages to
 * multiple partitions. As each ProducerImpl has it's own topic name and it is the initial unit of a single topic to
 * send messages, so use ProducerImpl as an enhanced class is an effective way.
 * <p>
 * About the enhanced methods, currently use {@link org.apache.pulsar.client.impl.ProducerImpl#sendAsync(
 *org.apache.pulsar.client.api.Message, org.apache.pulsar.client.impl.SendCallback)} as the enhanced method. Pulsar
 * provides users with two kinds of methods for sending messages sync methods and async methods. The async method use
 * {@link java.util.concurrent.CompletableFuture as the method result}, if use this method as the enhanced method is
 * hard to pass the snapshot of span, because can't ensure that the CompletableFuture is completed after the skywalking
 * dynamic field was updated. But execution time of sync method will be inaccurate.
 */
public class PulsarProducerInstrumentation extends BasePulsarProducerInstrumentation {

    public static final String CONSTRUCTOR_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.pulsar.ProducerConstructorInterceptor";

    @Override
    protected String[] witnessClasses() {
        return Constants.WITNESS_PULSAR_27X_CLASSES;
    }

    @Override
    protected String getCustomConstructorInterceptor() {
        return CONSTRUCTOR_INTERCEPTOR_CLASS;
    }
}
