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

import org.apache.skywalking.apm.plugin.pulsar.common.define.BasePulsarConsumerInstrumentation;

/**
 * The pulsar consumer instrumentation use {@link org.apache.pulsar.client.impl.ConsumerImpl} as an enhanced class.
 * {@link org.apache.pulsar.client.api.Consumer} is a user-oriented interface and the implementations are {@link
 * org.apache.pulsar.client.impl.ConsumerImpl} and {@link org.apache.pulsar.client.impl.MultiTopicsConsumerImpl}
 * <p>
 * The MultiTopicsConsumerImpl is a complex type with multiple ConsumerImpl to support uses receive messages from
 * multiple topics. As each ConsumerImpl has it's own topic name and it is the initial unit of a single topic to
 * receiving messages, so use ConsumerImpl as an enhanced class is an effective way.
 * <p>
 * Use <code>messageProcessed</code> as the enhanced method since pulsar consumer has multiple ways to receiving
 * messages such as sync method, async method and listeners. Method messageProcessed is a basic unit of ConsumerImpl, no
 * matter which way uses uses, messageProcessed will always record the message receiving.
 */
public class PulsarConsumerInstrumentation extends BasePulsarConsumerInstrumentation {

    @Override
    protected String[] witnessClasses() {
        return Constants.WITNESS_PULSAR_27X_CLASSES;
    }
}
