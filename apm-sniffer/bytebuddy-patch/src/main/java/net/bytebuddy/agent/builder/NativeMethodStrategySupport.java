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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

public class NativeMethodStrategySupport {

    private static ILog LOGGER = LogManager.getLogger(NativeMethodStrategySupport.class);

    public static MethodNameTransformer resolve() {
        return new MethodNameTransformer() {
            @Override
            public String transform(MethodDescription methodDescription) {
                return "_sw_origin$" + methodDescription.getInternalName();
            }
        };
    }

    public static void apply(Instrumentation instrumentation, ClassFileTransformer classFileTransformer) {
    }

    public static Object create(Class<?> type) throws Exception {
        // create the impl class of protected interface
        return new ByteBuddy().subclass(Object.class)
                .implement(type)
                .method(ElementMatchers.named("resolve"))
                .intercept(MethodDelegation.to(NativeMethodStrategySupport.class))
                .method(ElementMatchers.named("apply"))
                .intercept(MethodDelegation.to(NativeMethodStrategySupport.class))
                .make()
                .load(type.getClassLoader())
                .getLoaded()
                .newInstance();
    }

    public static void inject(AgentBuilder agentBuilder, Class clazz, String prefix) {
        try {
            Field nativeMethodStrategyField = clazz.getDeclaredField("nativeMethodStrategy");
            nativeMethodStrategyField.setAccessible(true);
            nativeMethodStrategyField.set(agentBuilder, new MyNativeMethodStrategy(prefix));
        } catch (Exception e) {
            LOGGER.error(e, "SkyWalking agent inject NativeMethodStrategy failure. clazz: " + clazz.getName());
        }
    }
}
