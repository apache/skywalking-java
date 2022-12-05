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

import io.micrometer.context.ThreadLocalAccessor;

/**
 * A {@link ThreadLocalAccessor} to put and restore current {@code ContextSnapshot} from APM agent.
 */
public class SkywalkingContextSnapshotThreadLocalAccessor implements ThreadLocalAccessor<Object> {

    /**
     * Key under which ContextSnapshot is being registered.
     */
    public static final String KEY = "skywalking.contextsnapshot";

    @Override
    public Object key() {
        return KEY;
    }

    // Type will be ContextSnapshot
    @Override
    public Object getValue() {
        return null;
    }

    // Object to set will be ContextSnapshot
    @Override
    public void setValue(Object value) {

    }

    @Override
    public void reset() {

    }

}
