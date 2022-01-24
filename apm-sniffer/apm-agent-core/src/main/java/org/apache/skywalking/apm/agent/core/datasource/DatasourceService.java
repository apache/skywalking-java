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

package org.apache.skywalking.apm.agent.core.datasource;

import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.util.RunnableWithExceptionProtection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The <code>DatasourceService</code> represents a timer, which register Datasource monitor metrics
 * {@link DatasourceRegister}
 */
@DefaultImplementor
public class DatasourceService implements BootService, Runnable {
    private static final ILog LOGGER = LogManager.getLogger(DatasourceService.class);
    private volatile ScheduledFuture<?> registerMetricFuture;
    private volatile List<DatasourceRegister> datasourceRegisters = new ArrayList<DatasourceRegister>();

    @Override
    public void prepare() throws Throwable {
        datasourceRegisters.add(new DruidRegister());
        datasourceRegisters.add(new HikaricpRegister());
    }

    @Override
    public void boot() throws Throwable {

        registerMetricFuture = Executors.newSingleThreadScheduledExecutor(
                new DefaultNamedThreadFactory("DatasourceService-Register"))
                .scheduleAtFixedRate(new RunnableWithExceptionProtection(
                        this,
                        new RunnableWithExceptionProtection.CallbackWhenException() {
                            @Override
                            public void handle(Throwable t) {
                                LOGGER.error("DatasourceService Register metrics failure.", t);
                            }
                        }
                ), 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        registerMetricFuture.cancel(true);
    }

    @Override
    public void run() {
        for (DatasourceRegister datasourceRegister : datasourceRegisters) {
            datasourceRegister.register();
        }
    }
}
