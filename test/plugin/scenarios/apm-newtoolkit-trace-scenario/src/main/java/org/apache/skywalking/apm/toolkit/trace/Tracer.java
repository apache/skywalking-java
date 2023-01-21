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

package org.apache.skywalking.apm.toolkit.trace;

public class Tracer {
    public static SpanRef createEntrySpan(String operationName, ContextCarrierRef carrierRef) {
        return new SpanRef();
    }

    public static SpanRef createLocalSpan(String operationName) {
        return new SpanRef();
    }

    public static SpanRef createExitSpan(String operationName, String remotePeer) {
        return new SpanRef();
    }

    public static SpanRef createExitSpan(String operationName, ContextCarrierRef carrierRef, String remotePeer) {
        return new SpanRef();
    }

    public static void stopSpan() {
    }

    public static void inject(ContextCarrierRef carrierRef) {
    }

    public static void extract(ContextCarrierRef carrierRef) {
    }

    public static ContextSnapshotRef capture() {
        return new ContextSnapshotRef();
    }

    public static void continued(ContextSnapshotRef snapshot) {
    }
}
