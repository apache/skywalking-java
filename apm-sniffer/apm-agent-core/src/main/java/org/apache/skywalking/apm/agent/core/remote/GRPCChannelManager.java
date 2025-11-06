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
import io.grpc.ConnectivityState;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.util.RunnableWithExceptionProtection;
import org.apache.skywalking.apm.util.StringUtil;

import static org.apache.skywalking.apm.agent.core.conf.Config.Collector.IS_RESOLVE_DNS_PERIODICALLY;

@DefaultImplementor
public class GRPCChannelManager implements BootService, Runnable {
    private static final ILog LOGGER = LogManager.getLogger(GRPCChannelManager.class);

    private volatile GRPCChannel managedChannel = null;
    private volatile ScheduledFuture<?> connectCheckFuture;
    private volatile boolean reconnect = true;
    private final Random random = new Random();
    private final List<GRPCChannelListener> listeners = Collections.synchronizedList(new LinkedList<>());
    private volatile List<String> grpcServers;
    private volatile int selectedIdx = -1;
    private volatile int reconnectCount = 0;
    private volatile int transientFailureCount = 0;
    private final Object statusLock = new Object();

    @Override
    public void prepare() {

    }

    @Override
    public void boot() {
        if (Config.Collector.BACKEND_SERVICE.trim().length() == 0) {
            LOGGER.error("Collector server addresses are not set.");
            LOGGER.error("Agent will not uplink any data.");
            return;
        }
        grpcServers = Arrays.asList(Config.Collector.BACKEND_SERVICE.split(","));
        connectCheckFuture = Executors.newSingleThreadScheduledExecutor(
            new DefaultNamedThreadFactory("GRPCChannelManager")
        ).scheduleAtFixedRate(
            new RunnableWithExceptionProtection(
                this,
                t -> LOGGER.error("unexpected exception.", t)
            ), 0, Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL, TimeUnit.SECONDS
        );
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void shutdown() {
        if (connectCheckFuture != null) {
            connectCheckFuture.cancel(true);
        }
        if (managedChannel != null) {
            managedChannel.shutdownNow();
        }
        LOGGER.debug("Selected collector grpc service shutdown.");
    }

    @Override
    public void run() {
        if (reconnect) {
            LOGGER.warn("Selected collector grpc service running, reconnect:{}.", reconnect);
        } else {
            LOGGER.debug("Selected collector grpc service running, reconnect:{}.", reconnect);
        }

        // Check channel state even when reconnect is false to detect prolonged failures
        checkChannelStateAndTriggerReconnectIfNeeded();

        if (IS_RESOLVE_DNS_PERIODICALLY && reconnect) {
            grpcServers = Arrays.stream(Config.Collector.BACKEND_SERVICE.split(","))
                    .filter(StringUtil::isNotBlank)
                    .map(eachBackendService -> eachBackendService.split(":"))
                    .filter(domainPortPairs -> {
                        if (domainPortPairs.length < 2) {
                            LOGGER.debug("Service address [{}] format error. The expected format is IP:port", domainPortPairs[0]);
                            return false;
                        }
                        return true;
                    })
                    .flatMap(domainPortPairs -> {
                        try {
                            return Arrays.stream(InetAddress.getAllByName(domainPortPairs[0]))
                                    .map(InetAddress::getHostAddress)
                                    .map(ip -> String.format("%s:%s", ip, domainPortPairs[1]));
                        } catch (Throwable t) {
                            LOGGER.error(t, "Failed to resolve {} of backend service.", domainPortPairs[0]);
                        }
                        return Stream.empty();
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (reconnect) {
            if (grpcServers.size() > 0) {
                String server = "";
                try {
                    int index = Math.abs(random.nextInt()) % grpcServers.size();

                    server = grpcServers.get(index);
                    String[] ipAndPort = server.split(":");

                    if (index != selectedIdx) {
                        selectedIdx = index;
                        LOGGER.debug("Connecting to different gRPC server {}. Shutting down existing channel if any.", server);
                        createNewChannel(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                    } else {
                        // Same server, increment reconnectCount and check state
                        reconnectCount++;

                        // Force reconnect if reconnectCount or transientFailureCount exceeds threshold
                        boolean forceReconnect = reconnectCount > Config.Agent.FORCE_RECONNECTION_PERIOD
                                              || transientFailureCount > Config.Agent.FORCE_RECONNECTION_PERIOD;

                        if (forceReconnect) {
                            // Failed to reconnect after multiple attempts, force rebuild channel
                            LOGGER.warn("Force rebuild channel to {} (reconnectCount={}, transientFailureCount={})",
                                      server, reconnectCount, transientFailureCount);
                            createNewChannel(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                        } else if (managedChannel.isConnected(false)) {
                            // Reconnect to the same server is automatically done by GRPC,
                            // therefore we are responsible to check the connectivity and
                            // set the state and notify listeners
                            markAsConnected();
                        }
                    }

                    return;
                } catch (Throwable t) {
                    LOGGER.error(t, "Create channel to {} fail.", server);
                }
            }

            LOGGER.debug(
                "Selected collector grpc service is not available. Wait {} seconds to retry",
                Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL
            );
        }
    }

    public void addChannelListener(GRPCChannelListener listener) {
        listeners.add(listener);
    }

    public Channel getChannel() {
        return managedChannel.getChannel();
    }

    /**
     * If the given exception is triggered by network problem, connect in background.
     */
    public void reportError(Throwable throwable) {
        if (isNetworkError(throwable)) {
            triggerReconnect();
        }
    }

    private void notify(GRPCChannelStatus status) {
        synchronized (listeners) {
            for (GRPCChannelListener listener : listeners) {
                try {
                    listener.statusChanged(status);
                } catch (Throwable t) {
                    LOGGER.error(t, "Fail to notify {} about channel connected.", listener.getClass().getName());
                }
            }
        }
    }

    /**
     * Create a new gRPC channel to the specified server and reset connection state.
     */
    private void createNewChannel(String host, int port) throws Exception {
        if (managedChannel != null) {
            managedChannel.shutdownNow();
        }

        managedChannel = GRPCChannel.newBuilder(host, port)
                                    .addManagedChannelBuilder(new StandardChannelBuilder())
                                    .addManagedChannelBuilder(new TLSChannelBuilder())
                                    .addChannelDecorator(new AgentIDDecorator())
                                    .addChannelDecorator(new AuthenticationDecorator())
                                    .build();

        markAsConnected();
    }

    /**
     * Trigger reconnection by setting reconnect flag and notifying listeners.
     */
    private void triggerReconnect() {
        synchronized (statusLock) {
            reconnect = true;
            notify(GRPCChannelStatus.DISCONNECT);
        }
    }

    /**
     * Mark connection as successful and reset connection state.
     */
    private void markAsConnected() {
        synchronized (statusLock) {
            reconnectCount = 0;
            reconnect = false;
            notify(GRPCChannelStatus.CONNECTED);
        }
    }

    /**
     * Check the connectivity state of existing channel and trigger reconnect if needed.
     * This method monitors TRANSIENT_FAILURE state and triggers reconnect if the failure persists too long.
     */
    private void checkChannelStateAndTriggerReconnectIfNeeded() {
        if (managedChannel != null) {
            try {
                ConnectivityState state = managedChannel.getState(false);
                LOGGER.debug("Current channel state: {}", state);

                if (state == ConnectivityState.TRANSIENT_FAILURE) {
                    transientFailureCount++;
                    LOGGER.warn("Channel in TRANSIENT_FAILURE state, count: {}", transientFailureCount);
                } else if (state == ConnectivityState.SHUTDOWN) {
                    LOGGER.warn("Channel is SHUTDOWN");
                    if (!reconnect) {
                        triggerReconnect();
                    }
                } else {
                    // IDLE, READY, CONNECTING are all normal states
                    transientFailureCount = 0;
                }
            } catch (Throwable t) {
                LOGGER.error(t, "Error checking channel state");
            }
        }
    }

    private boolean isNetworkError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException) {
            StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;
            return statusEquals(
                statusRuntimeException.getStatus(), Status.UNAVAILABLE, Status.PERMISSION_DENIED,
                Status.UNAUTHENTICATED, Status.RESOURCE_EXHAUSTED, Status.UNKNOWN
            );
        }
        return false;
    }

    private boolean statusEquals(Status sourceStatus, Status... potentialStatus) {
        for (Status status : potentialStatus) {
            if (sourceStatus.getCode() == status.getCode()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }
}
