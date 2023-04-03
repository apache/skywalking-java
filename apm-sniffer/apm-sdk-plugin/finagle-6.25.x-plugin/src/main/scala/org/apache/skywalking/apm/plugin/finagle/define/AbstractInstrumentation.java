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

package org.apache.skywalking.apm.plugin.finagle.define;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;

public abstract class AbstractInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    /**
     * this class exists in version 6.25.0 and above
     */
    private static final String ADDRMETADATAEXTRACTION_CLASS = "com.twitter.finagle.client.AddrMetadataExtraction";

    @Override
    protected String[] witnessClasses() {
        return new String[]{ADDRMETADATAEXTRACTION_CLASS};
    }
}
