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

package org.apache.skywalking.apm.plugin.grpc.v1;

/**
 * Constant variables
 */
public class Constants {

    /**
     * Mark the current application is the gRPC client in current tracing span.
     */
    public static final String CLIENT = "/client";

    /**
     * Mark the current application is the gRPC server in current tracing span.
     */
    public static final String SERVER = "/server";

    /**
     * Operation name for request message received on server or sent on client.
     * <p>
     * Spans of this operations just be create with request stream calls.
     */
    public static final String REQUEST_ON_MESSAGE_OPERATION_NAME = "/Request/onMessage";

    /**
     * Operation name for request completed all message sending.
     * <p>
     * However, the call may still be cancelled.
     */
    public static final String REQUEST_ON_HALF_CLOSE_OPERATION_NAME = "/Request/onHalfClose";

    /**
     * Operation name for client has completed request sending, there are no more incoming request.
     * <p>
     * It should happen with half close state usually.
     */
    public static final String REQUEST_ON_COMPLETE_OPERATION_NAME = "/Request/onComplete";

    /**
     * Operation name for client has cancelled the call.
     */
    public static final String REQUEST_ON_CANCEL_OPERATION_NAME = "/Request/onCancel";

    /**
     * Operation name for response message received on client or sent on server.
     * <p>
     * Spans of this operations just be create with response stream calls.
     */
    public static final String RESPONSE_ON_MESSAGE_OPERATION_NAME = "/Response/onMessage";

    /**
     * Operation name for call closed with status and trailers.
     * <p>
     * Exceptions will be logs here.
     */
    public static final String RESPONSE_ON_CLOSE_OPERATION_NAME = "/Response/onClose";

    public static final String BLOCKING_CALL_EXIT_SPAN = "SW_GRPC_BLOCKING_CALL_EXIT_SPAN";

    public static final String GENERIC_CALL_METHOD = "GENERIC_CALL_METHOD";

    public static final String CLIENT_STREAM_PEER = "CLIENT_STREAM_PEER";
}