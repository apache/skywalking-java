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

package org.apache.skywalking.apm.plugin.spring.ai.v1;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AbstractObservationVectorStoreConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        if (allArguments != null && allArguments.length > 0) {
            String embeddingModelName = resolveModelFromArgument(allArguments[0]);
            objInst.setSkyWalkingDynamicField(new VectorStoreEnhanceContext(embeddingModelName));
        }
    }

    private String resolveModelFromArgument(Object argument) {
        if (argument instanceof EmbeddingModel) {
            return resolveModelFromEmbeddingModel(argument);
        }
        return null;
    }

    private String resolveModelFromEmbeddingModel(Object embeddingModel) {
        if (embeddingModel == null) {
            return null;
        }
        String model = resolveModelFromOptionsMethod(embeddingModel);
        if (StringUtils.hasText(model)) {
            return model;
        }
        model = resolveModelFromOptionsField(embeddingModel, "options");
        if (StringUtils.hasText(model)) {
            return model;
        }
        return resolveModelFromOptionsField(embeddingModel, "defaultOptions");
    }

    private String resolveModelFromOptionsMethod(Object embeddingModel) {
        try {
            Method method = embeddingModel.getClass().getMethod("getOptions");
            return resolveModelFromOptions(method.invoke(embeddingModel));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveModelFromOptionsField(Object embeddingModel, String fieldName) {
        Class<?> type = embeddingModel.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return resolveModelFromOptions(field.get(embeddingModel));
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveModelFromOptions(Object options) {
        if (options instanceof EmbeddingOptions) {
            return ((EmbeddingOptions) options).getModel();
        }
        return null;
    }
}
