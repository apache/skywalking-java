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

package org.apache.skywalking.apm.agent.core.remote;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.conf.Config.Collector;
import org.apache.skywalking.apm.agent.core.conf.Config.Log;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.util.CollectionUtil;
import org.apache.skywalking.apm.commons.datacarrier.DataCarrier;
import org.apache.skywalking.apm.commons.datacarrier.buffer.BufferStrategy;
import org.apache.skywalking.apm.commons.datacarrier.consumer.IConsumer;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.logging.v3.LogData;
import org.apache.skywalking.apm.network.logging.v3.LogReportServiceGrpc;

@DefaultImplementor
public class LogReportServiceClient implements BootService, GRPCChannelListener, IConsumer<LogData.Builder> {
    private static final ILog LOGGER = LogManager.getLogger(LogReportServiceClient.class);

    private volatile DataCarrier<LogData.Builder> carrier;
    private volatile GRPCChannelStatus status;

    private volatile LogReportServiceGrpc.LogReportServiceStub logReportServiceStub;

    @Override
    public void prepare() throws Throwable {
        ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(this);
    }

    @Override
    public void boot() throws Throwable {
        carrier = new DataCarrier<>("gRPC-log", "gRPC-log",
                                    Config.Buffer.CHANNEL_SIZE,
                                    Config.Buffer.BUFFER_SIZE,
                                    BufferStrategy.IF_POSSIBLE
        );
        carrier.consume(this, 1);
    }

    @Override
    public void onComplete() throws Throwable {

    }

    public void produce(LogData.Builder logData) {
        if (Objects.nonNull(logData) && !carrier.produce(logData)) {
            if (LOGGER.isDebugEnable()) {
                LOGGER.debug("One log has been abandoned, cause by buffer is full.");
            }
        }
    }

    @Override
    public void init(final Properties properties) {

    }

    @Override
    public void consume(final List<LogData.Builder> dataList) {
        if (CollectionUtil.isEmpty(dataList)) {
            return;
        }

        if (GRPCChannelStatus.CONNECTED.equals(status)) {
            GRPCStreamServiceStatus status = new GRPCStreamServiceStatus(false);

            StreamObserver<LogData> logDataStreamObserver = logReportServiceStub
                .withDeadlineAfter(Collector.GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS)
                .collect(
                    new StreamObserver<Commands>() {
                        @Override
                        public void onNext(final Commands commands) {

                        }

                        @Override
                        public void onError(final Throwable throwable) {
                            status.finished();
                            LOGGER.error(throwable, "Try to send {} log data to collector, with unexpected exception.",
                                         dataList.size()
                            );
                            ServiceManager.INSTANCE
                                .findService(GRPCChannelManager.class)
                                .reportError(throwable);
                        }

                        @Override
                        public void onCompleted() {
                            status.finished();
                        }
                    });

            boolean isFirst = true;
            for (final LogData.Builder logData : dataList) {
                if (isFirst) {
                    // Only set service name of the first element in one stream
                    // https://github.com/apache/skywalking-data-collect-protocol/blob/master/logging/Logging.proto
                    // Log collecting protocol defines LogData#service is required in the first element only.
                    logData.setService(Config.Agent.SERVICE_NAME);
                    isFirst = false;
                }
                logDataStreamObserver.onNext(logData.build());
            }
            logDataStreamObserver.onCompleted();
            status.wait4Finish();
        }
    }

    @Override
    public void onError(final List<LogData.Builder> data, final Throwable t) {
        LOGGER.error(t, "Try to consume {} log data to sender, with unexpected exception.", data.size());
    }

    @Override
    public void onExit() {

    }

    @Override
    public void statusChanged(GRPCChannelStatus status) {
        if (GRPCChannelStatus.CONNECTED.equals(status)) {
            Channel channel = ServiceManager.INSTANCE.findService(GRPCChannelManager.class).getChannel();
            logReportServiceStub = LogReportServiceGrpc.newStub(channel)
                                                       .withMaxOutboundMessageSize(Log.MAX_MESSAGE_SIZE);
        }
        this.status = status;
    }

    @Override
    public void shutdown() {
        carrier.shutdownConsumers();
    }
}
