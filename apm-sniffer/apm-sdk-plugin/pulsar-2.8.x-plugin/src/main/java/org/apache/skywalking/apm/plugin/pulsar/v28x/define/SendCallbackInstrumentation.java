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
import org.apache.skywalking.apm.plugin.pulsar.common.define.BaseSendCallbackInstrumentation;

/**
 * Pulsar producer send callback instrumentation.
 * <p>
 * The send callback enhanced object will use {@link org.apache.skywalking.apm.plugin.pulsar.common.SendCallbackEnhanceRequiredInfo}
 * which {@link org.apache.skywalking.apm.plugin.pulsar.common.PulsarProducerInterceptor} set by skywalking dynamic
 * field of
 * enhanced object.
 * <p>
 * When a callback is complete, {@link org.apache.skywalking.apm.plugin.pulsar.common.SendCallbackInterceptor} will
 * continue
 * the {@link org.apache.skywalking.apm.plugin.pulsar.common.SendCallbackEnhanceRequiredInfo#getContextSnapshot()}.
 */
public class SendCallbackInstrumentation extends BaseSendCallbackInstrumentation {

    @Override
    protected List<WitnessMethod> witnessMethods() {
        return Collections.singletonList(Constants.WITNESS_PULSAR_28X_METHOD);
    }
}
