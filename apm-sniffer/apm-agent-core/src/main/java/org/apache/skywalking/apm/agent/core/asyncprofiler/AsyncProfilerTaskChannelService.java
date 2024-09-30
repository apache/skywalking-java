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

package org.apache.skywalking.apm.agent.core.asyncprofiler;

import io.grpc.Channel;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.commands.CommandService;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelListener;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelManager;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelStatus;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfileTaskCommandQuery;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerTaskGrpc;
import org.apache.skywalking.apm.util.RunnableWithExceptionProtection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.skywalking.apm.agent.core.conf.Config.Collector.GRPC_UPSTREAM_TIMEOUT;

@DefaultImplementor
public class AsyncProfilerTaskChannelService implements BootService, Runnable, GRPCChannelListener {
    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTaskChannelService.class);

    // channel status
    private volatile GRPCChannelStatus status = GRPCChannelStatus.DISCONNECT;
    private volatile AsyncProfilerTaskGrpc.AsyncProfilerTaskBlockingStub asyncProfilerTaskBlockingStub;

    // query task list schedule
    private volatile ScheduledFuture<?> getTaskListFuture;

    @Override
    public void run() {
        if (status == GRPCChannelStatus.CONNECTED) {
            // test start command and 10s after put stop command
            long lastCommandCreateTime = ServiceManager.INSTANCE
                    .findService(AsyncProfilerTaskExecutionService.class).getLastCommandCreateTime();
            AsyncProfileTaskCommandQuery query = AsyncProfileTaskCommandQuery.newBuilder()
                    .setServiceInstance(Config.Agent.INSTANCE_NAME)
                    .setService(Config.Agent.SERVICE_NAME)
                    .setLastCommandTime(lastCommandCreateTime)
                    .build();
            Commands commands = asyncProfilerTaskBlockingStub.withDeadlineAfter(GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS)
                    .getAsyncProfileTaskCommands(query);
            ServiceManager.INSTANCE.findService(CommandService.class).receiveCommand(commands);
        }
    }

    @Override
    public void statusChanged(GRPCChannelStatus status) {
        if (GRPCChannelStatus.CONNECTED.equals(status)) {
            Channel channel = ServiceManager.INSTANCE.findService(GRPCChannelManager.class).getChannel();
            asyncProfilerTaskBlockingStub = AsyncProfilerTaskGrpc.newBlockingStub(channel);
        } else {
            asyncProfilerTaskBlockingStub = null;
        }
        this.status = status;
    }

    @Override
    public void prepare() throws Throwable {
        ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(this);
    }

    @Override
    public void boot() throws Throwable {

        if (Config.AsyncProfiler.ACTIVE) {
            getTaskListFuture = Executors.newSingleThreadScheduledExecutor(
                    new DefaultNamedThreadFactory("AsyncProfilerGetTaskService")
            ).scheduleWithFixedDelay(
                    new RunnableWithExceptionProtection(
                            this,
                            t -> LOGGER.error("Query async profiler task list failure.", t)
                    ), 0, Config.Collector.GET_PROFILE_TASK_INTERVAL, TimeUnit.SECONDS
            );
        }
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        if (getTaskListFuture != null) {
            getTaskListFuture.cancel(true);
        }
    }
}
