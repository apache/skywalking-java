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

package org.apache.skywalking.apm.plugin.nats.client.define;

import java.util.Collections;
import java.util.List;
import org.apache.skywalking.apm.agent.core.plugin.WitnessMethod;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;

import static net.bytebuddy.matcher.ElementMatchers.named;

public abstract class AbstractWitnessInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    /*
     * Currently, we only support 2.14.x-2.16.5, as there is no proper way and opportunity
     * to change the message header and re-calculate the message length for 2.16.5+ yet.
     * This method prevents users from applying this plugin to unsupported versions,
     * which may cause unknown errors.
     * For more information: https://github.com/apache/skywalking/discussions/11650
     */
    @Override
    protected List<WitnessMethod> witnessMethods() {
        return Collections.singletonList(new WitnessMethod(
            "io.nats.client.impl.NatsMessage",
            named("calculateIfDirty")
        ));
    }
}
