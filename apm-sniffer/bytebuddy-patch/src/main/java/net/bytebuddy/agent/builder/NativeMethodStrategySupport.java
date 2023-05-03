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

package net.bytebuddy.agent.builder;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.lang.reflect.Field;

/**
 * Inject custom NativeMethodStrategy to AgentBuilder
 */
public class NativeMethodStrategySupport {
    private static final String PREFIX = "origin$";
    private static ILog LOGGER = LogManager.getLogger(NativeMethodStrategySupport.class);

    public static void inject(AgentBuilder agentBuilder, String nameTrait) {
        String prefix = nameTrait + "_" + PREFIX;
        Class<? extends AgentBuilder> clazz = agentBuilder.getClass();
        if (clazz != AgentBuilder.Default.class) {
            throw new IllegalStateException("Only accept original AgentBuilder instance but not a wrapper instance: " + clazz.getName());
        }
        try {
            Field nativeMethodStrategyField = clazz.getDeclaredField("nativeMethodStrategy");
            nativeMethodStrategyField.setAccessible(true);
            nativeMethodStrategyField.set(agentBuilder, new SWNativeMethodStrategy(prefix));
        } catch (Exception e) {
            LOGGER.error(e, "SkyWalking agent inject NativeMethodStrategy failure. clazz: " + clazz.getName());
        }
    }
}
