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

package org.apache.skywalking.apm.toolkit.micrometer.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.transport.ReceiverContext;

public class SkywalkingReceiverTracingHandler implements ObservationHandler<ReceiverContext<?>> {
    @Override
    public boolean supportsContext(final Observation.Context context) {
        return context instanceof ReceiverContext;
    }

    @Override
    public void onStart(final ReceiverContext<?> context) {

    }

    @Override
    public void onError(final ReceiverContext<?> context) {

    }

    @Override
    public void onEvent(final Observation.Event event, final ReceiverContext<?> context) {

    }

    @Override
    public void onScopeOpened(final ReceiverContext<?> context) {

    }

    @Override
    public void onScopeClosed(final ReceiverContext<?> context) {

    }

    @Override
    public void onStop(final ReceiverContext<?> context) {

    }
}
