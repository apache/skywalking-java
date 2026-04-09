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

package org.apache.skywalking.apm.plugin.jedis.v4.define;

import org.apache.skywalking.apm.agent.core.plugin.WitnessMethod;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;

import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public abstract class AbstractWitnessInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    @Override
    protected String[] witnessClasses() {
        return new String[0];
    }

    @Override
    protected List<WitnessMethod> witnessMethods() {
        // Connection.executeCommand(CommandObject) exists in Jedis 4.x+ and 5.x,
        // but not in 3.x. Previous witness Pipeline.persist(1) broke in 5.x
        // because persist moved from Pipeline to PipeliningBase parent class.
        return Collections.singletonList(new WitnessMethod(
                "redis.clients.jedis.Connection",
                named("executeCommand").and(takesArguments(1))));
    }
}
