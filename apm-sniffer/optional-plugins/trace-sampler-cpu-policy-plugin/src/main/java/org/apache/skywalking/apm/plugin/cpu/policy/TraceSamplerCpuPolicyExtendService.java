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

package org.apache.skywalking.apm.plugin.cpu.policy;

import org.apache.skywalking.apm.agent.core.boot.OverrideImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.jvm.JVMService;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.sampling.SamplingService;
import org.apache.skywalking.apm.plugin.cpu.policy.conf.TraceSamplerCpuPolicyPluginConfig;

@OverrideImplementor(SamplingService.class)
public class TraceSamplerCpuPolicyExtendService extends SamplingService {
    private static final ILog LOGGER = LogManager.getLogger(TraceSamplerCpuPolicyExtendService.class);

    private volatile boolean cpuUsagePercentLimitOn = false;
    private volatile JVMService jvmService;

    @Override
    public void prepare() {
        super.prepare();
    }

    @Override
    public void boot() {
        super.boot();
        if (TraceSamplerCpuPolicyPluginConfig.Plugin.CpuPolicy.SAMPLE_CPU_USAGE_PERCENT_LIMIT > 0) {
            LOGGER.info("TraceSamplerCpuPolicyExtendService cpu usage percent limit open");
            jvmService = ServiceManager.INSTANCE.findService(JVMService.class);
            cpuUsagePercentLimitOn = true;
        }
    }

    @Override
    public void onComplete() {
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public boolean trySampling(final String operationName) {
        if (cpuUsagePercentLimitOn) {
            double cpuUsagePercent = jvmService.getCpuUsagePercent();
            if (cpuUsagePercent > TraceSamplerCpuPolicyPluginConfig.Plugin.CpuPolicy.SAMPLE_CPU_USAGE_PERCENT_LIMIT) {
                return false;
            }
        }
        return super.trySampling(operationName);
    }

    @Override
    public void forceSampled() {
        super.forceSampled();
    }

}
