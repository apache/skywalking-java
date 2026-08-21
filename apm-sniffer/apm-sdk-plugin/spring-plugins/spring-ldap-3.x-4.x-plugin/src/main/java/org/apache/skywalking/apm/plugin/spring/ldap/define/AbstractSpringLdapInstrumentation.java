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

package org.apache.skywalking.apm.plugin.spring.ldap.define;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.ClassInstanceMethodsEnhancePluginDefineV2;

/**
 * Spring LDAP 3.3+ witness. {@code ObservationContextSource} was added in 3.3.0 and is
 * packaged in {@code spring-ldap-core}, so it gates the plugin to the documented range
 * and avoids weaving Spring LDAP 2.x {@code LdapTemplate}.
 */
public abstract class AbstractSpringLdapInstrumentation extends ClassInstanceMethodsEnhancePluginDefineV2 {

    static final String OBSERVATION_CONTEXT_SOURCE =
        "org.springframework.ldap.core.support.ObservationContextSource";

    @Override
    protected final String[] witnessClasses() {
        return new String[] {
            OBSERVATION_CONTEXT_SOURCE
        };
    }
}
