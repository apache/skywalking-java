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

package org.apache.skywalking.apm.plugin.grpc.v1.server;

import io.grpc.ServerBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

/**
 * {@link AbstractServerImplBuilderInterceptor} add the {@link ServerInterceptor} interceptor for every ServerService.
 */
public class AbstractServerImplBuilderInterceptor implements InstanceMethodsAroundInterceptor {
    private final static Map<Class<?>, Field> FIELD_CACHE = new HashMap<>();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        if (objInst.getSkyWalkingDynamicField() == null) {
            ServerBuilder<?> builder = (ServerBuilder) objInst;
            Field field = findField(builder.getClass());
            if (field != null) {
                List<?> interceptors = (List<?>) field.get(builder);
                boolean hasCustomInterceptor = interceptors.stream()
                        .anyMatch(i -> i.getClass() == ServerInterceptor.class);

                if (!hasCustomInterceptor) {
                    ServerInterceptor interceptor = new ServerInterceptor();
                    builder.intercept(interceptor);
                    objInst.setSkyWalkingDynamicField(interceptor);
                }

            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {

    }

    private static Field findField(Class<?> clazz) {
        if (FIELD_CACHE.containsKey(clazz)) {
            return FIELD_CACHE.get(clazz);
        }
        synchronized (AbstractServerImplBuilderInterceptor.class) {
            if (FIELD_CACHE.containsKey(clazz)) {
                return FIELD_CACHE.get(clazz);
            }
            Field field = doFindField(clazz);
            FIELD_CACHE.put(clazz, field);
            return field;
        }
    }

    private static Field doFindField(Class<?> clazz) {
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals("interceptors")) {
                    f.setAccessible(true);
                    return f;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
