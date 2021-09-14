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

package org.apache.skywalking.apm.plugin.thrift;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.thrift.ProcessFunction;
import org.apache.thrift.TBaseAsyncProcessor;
import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.TProcessor;

public class TMultiplexedProcessorRegisterDefaultInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog LOGGER = LogManager.getLogger(TMultiplexedProcessorRegisterDefaultInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst,
                             Method method,
                             Object[] allArguments,
                             Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

        Map<String, ProcessFunction> processMap = (Map<String, ProcessFunction>) objInst.getSkyWalkingDynamicField();
        TProcessor processor = (TProcessor) allArguments[0];
        processMap.putAll(getProcessMap(processor));
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst,
                              Method method,
                              Object[] allArguments,
                              Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst,
                                      Method method,
                                      Object[] allArguments,
                                      Class<?>[] argumentsTypes,
                                      Throwable t) {
    }

    private Map<String, ProcessFunction> getProcessMap(TProcessor processor) {
        Map<String, ProcessFunction> hashMap = new HashMap<>();
        if (processor instanceof TBaseProcessor) {
            Map<String, ProcessFunction> processMapView = ((TBaseProcessor) processor).getProcessMapView();
            hashMap.putAll(processMapView);
        } else if (processor instanceof TBaseAsyncProcessor) {
            Map<String, ProcessFunction> processMapView = ((TBaseProcessor) processor).getProcessMapView();
            hashMap.putAll(processMapView);
        } else {
            LOGGER.warn("Not support this processor:{}", processor.getClass().getName());
        }
        return hashMap;
    }
}
