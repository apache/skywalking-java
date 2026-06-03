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
import org.apache.skywalking.apm.plugin.spring.ai.v1.common.EmbeddingModelEnhanceContext;

public class AbstractObservationVectorStoreConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        objInst.setSkyWalkingDynamicField(new VectorStoreEnhanceContext(resolveContextFromArgument(allArguments[0])));
    }

    private EmbeddingModelEnhanceContext resolveContextFromArgument(Object argument) {
        if (argument instanceof EnhancedInstance) {
            return getOrCreateContext((EnhancedInstance) argument);
        }
        return null;
    }

    private EmbeddingModelEnhanceContext getOrCreateContext(EnhancedInstance embeddingModel) {
        Object context = embeddingModel.getSkyWalkingDynamicField();
        if (context instanceof EmbeddingModelEnhanceContext) {
            return (EmbeddingModelEnhanceContext) context;
        }
        EmbeddingModelEnhanceContext embeddingModelEnhanceContext = new EmbeddingModelEnhanceContext();
        embeddingModel.setSkyWalkingDynamicField(embeddingModelEnhanceContext);
        return embeddingModelEnhanceContext;
    }
}
