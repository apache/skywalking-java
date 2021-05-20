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

package org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.v2.StaticMethodsInterceptV2Point;

/**
 * Plugins, which only need enhance class instance methods. Actually, inherit from {@link
 * ClassInstanceMethodsEnhancePluginDefineV2} has no differences with inherit from {@link ClassEnhancePluginDefineV2}.
 * Just override {@link ClassEnhancePluginDefineV2#getStaticMethodsInterceptPoints}, and return NULL, which means nothing
 * to enhance.
 */
public abstract class ClassInstanceMethodsEnhancePluginDefineV2 extends ClassEnhancePluginDefineV2 {

    /**
     * @return null, means enhance no v2 static methods.
     */
    @Override
    public StaticMethodsInterceptV2Point[] getStaticMethodsInterceptV2Points() {
        return null;
    }
}
