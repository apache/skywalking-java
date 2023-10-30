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

import net.bytebuddy.agent.builder.AgentBuilder.PoolStrategy.WithTypePoolCache.Simple;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A DescriptionStrategy to implement the cache first policy,
 * to get the original class description and avoid unnecessary bytecode parsing.
 */
public class SWDescriptionStrategy implements AgentBuilder.DescriptionStrategy {

    private Simple typePoolCache = new Simple(new ConcurrentHashMap<>());

    private AgentBuilder.DescriptionStrategy delegate = AgentBuilder.DescriptionStrategy.Default.HYBRID;

    @Override
    public boolean isLoadedFirst() {
        return true;
    }

    @Override
    public TypeDescription apply(String name,
                                 @MaybeNull Class<?> type,
                                 TypePool typePool,
                                 AgentBuilder.CircularityLock circularityLock,
                                 @MaybeNull ClassLoader classLoader,
                                 @MaybeNull JavaModule module) {
        // use cache first
        TypePool.CacheProvider cacheProvider = getCacheProvider(classLoader);
        TypePool.Resolution resolution = cacheProvider.find(name);
        if (resolution != null && resolution.isResolved()) {
            return resolution.resolve();
        }

        // do as AgentBuilder.DescriptionStrategy.Default.HYBRID
        TypeDescription typeDescription = delegate.apply(name, type, typePool, circularityLock, classLoader, module);

        // cache it
        cacheProvider.register(name, new TypePool.Resolution.Simple(typeDescription));
        return typeDescription;
    }

    protected TypePool.CacheProvider getCacheProvider(ClassLoader classLoader) {
        return typePoolCache.locate(classLoader);
    }

}
