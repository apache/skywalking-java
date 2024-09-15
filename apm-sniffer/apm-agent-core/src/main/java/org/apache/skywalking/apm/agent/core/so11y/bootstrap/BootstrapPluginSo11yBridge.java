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

package org.apache.skywalking.apm.agent.core.so11y.bootstrap;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.BootstrapInterRuntimeAssist;
import org.apache.skywalking.apm.agent.core.so11y.AgentSo11y;

/**
 * used by {@link BootstrapInterRuntimeAssist}
 */
@SuppressWarnings("unused")
public class BootstrapPluginSo11yBridge implements BootstrapPluginSo11y {

    public static BootstrapPluginSo11y getSo11y() {
        return new BootstrapPluginSo11yBridge();
    }

    private BootstrapPluginSo11yBridge() {
    }

    @Override
    public void duration(final double timeCostInNanos) {
        AgentSo11y.durationOfInterceptor(timeCostInNanos);
    }

    @Override
    public void error(final String pluginName, final String interType) {
        AgentSo11y.errorOfPlugin(pluginName, interType);
    }
}
