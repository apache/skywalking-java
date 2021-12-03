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

import org.apache.skywalking.apm.plugin.pulsar.common.define.BasePulsarConsumerListenerInstrumentation;

/**
 * The pulsar consumer listener instrumentation use {@link org.apache.pulsar.client.api.MessageListener} as an enhanced
 * class.
 * <p>
 * User will implement {@link org.apache.pulsar.client.api.MessageListener} interface to consume message and enhance
 * all instances of {@link org.apache.pulsar.client.api.MessageListener} interface can let users get trace information
 * in message listener thread.
 */
public class PulsarConsumerListenerInstrumentation extends BasePulsarConsumerListenerInstrumentation {

    @Override
    protected String[] witnessClasses() {
        return Constants.WITNESS_PULSAR_27X_CLASSES;
    }
}
