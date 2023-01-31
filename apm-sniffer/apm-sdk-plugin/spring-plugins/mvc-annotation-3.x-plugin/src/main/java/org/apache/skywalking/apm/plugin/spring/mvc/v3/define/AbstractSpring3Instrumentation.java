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

package org.apache.skywalking.apm.plugin.spring.mvc.v3.define;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;

/**
 * {@link AbstractSpring3Instrumentation} define witness classes of the spring mvc 3 plugin. all Instrumentations
 * extends this class.
 */
public abstract class AbstractSpring3Instrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    public static final String WITHNESS_CLASSES = "org.springframework.web.servlet.view.xslt.AbstractXsltView";

    @Override
    protected final String[] witnessClasses() {
        return new String[] {WITHNESS_CLASSES};
    }
}
