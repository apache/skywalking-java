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

package org.apache.skywalking.oap.server.recevier.configuration.discovery;

import org.apache.skywalking.oap.server.configuration.api.ConfigurationModule;
import org.apache.skywalking.oap.server.configuration.api.DynamicConfigurationService;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.skywalking.oap.server.receiver.sharing.server.SharingServerModule;
import org.apache.skywalking.oap.server.recevier.configuration.discovery.handler.grpc.ConfigurationDiscoveryServiceHandler;

public class ConfigurationDiscoveryProvider extends ModuleProvider {

    private AgentConfigurationsWatcher agentConfigurationsWatcher;
    private ConfigurationDiscoveryModuleConfig configurationDiscoveryModuleConfig;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return ConfigurationDiscoveryModule.class;
    }

    public ConfigurationDiscoveryProvider() {
        configurationDiscoveryModuleConfig = new ConfigurationDiscoveryModuleConfig();
    }

    @Override
    public ModuleConfig createConfigBeanIfAbsent() {
        return configurationDiscoveryModuleConfig;
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        agentConfigurationsWatcher = new AgentConfigurationsWatcher(this);
    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {
        DynamicConfigurationService dynamicConfigurationService = getManager().find(ConfigurationModule.NAME)
                                                                              .provider()
                                                                              .getService(
                                                                                  DynamicConfigurationService.class);
        dynamicConfigurationService.registerConfigChangeWatcher(agentConfigurationsWatcher);

        /*
         * Register ConfigurationDiscoveryServiceHandler to process gRPC requests for ConfigurationDiscovery.
         */
        GRPCHandlerRegister grpcHandlerRegister = getManager().find(SharingServerModule.NAME)
                                                              .provider()
                                                              .getService(GRPCHandlerRegister.class);
        grpcHandlerRegister.addHandler(new ConfigurationDiscoveryServiceHandler(
            agentConfigurationsWatcher,
            configurationDiscoveryModuleConfig.isDisableMessageDigest()
        ));
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {
    }

    @Override
    public String[] requiredModules() {
        return new String[] {
            ConfigurationModule.NAME,
            SharingServerModule.NAME
        };
    }
}
