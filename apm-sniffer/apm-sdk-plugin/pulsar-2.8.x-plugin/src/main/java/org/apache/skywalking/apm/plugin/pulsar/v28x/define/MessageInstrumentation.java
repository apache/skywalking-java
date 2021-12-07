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

package org.apache.skywalking.apm.plugin.pulsar.v28x.define;

import java.util.Collections;
import java.util.List;
import org.apache.skywalking.apm.agent.core.plugin.WitnessMethod;
import org.apache.skywalking.apm.plugin.pulsar.common.define.BaseMessageInstrumentation;

/**
 * Pulsar message instrumentation.
 * <p>
 * The message enhanced object is only for passing message reception span across threads.
 * <p>
 * Enhanced message object will be injected {@link org.apache.skywalking.apm.plugin.pulsar.common.MessageEnhanceRequiredInfo}
 * after message process method if consumer has a message listener.
 * </p>
 */
public class MessageInstrumentation extends BaseMessageInstrumentation {

    @Override
    protected List<WitnessMethod> witnessMethods() {
        return Collections.singletonList(Constants.WITNESS_PULSAR_28X_METHOD);
    }
}
