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

package org.apache.skywalking.apm.plugin.lettuce.common;

import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.RedisCommand;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;

public class RedisCommandCompleteMethodInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) {
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) {
        RedisWriteSpan writeSpan = RedisCommandEnhanceInfo.takeWriteSpan(objInst);
        if (writeSpan != null) {
            // A reactive command is completed without an exception on an error reply, such as WRONGTYPE, while an
            // AsyncCommand completes its command exceptionally before.
            CommandOutput<?, ?, ?> output = ((RedisCommand<?, ?, ?>) objInst).getOutput();
            if (output != null && output.hasError()) {
                writeSpan.errorReply(String.valueOf(output.getError()));
            }
            writeSpan.finishCommand();
            objInst.setSkyWalkingDynamicField(null);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        RedisWriteSpan writeSpan = RedisCommandEnhanceInfo.getWriteSpan(objInst);
        if (writeSpan != null) {
            writeSpan.log(t);
        }
    }
}
