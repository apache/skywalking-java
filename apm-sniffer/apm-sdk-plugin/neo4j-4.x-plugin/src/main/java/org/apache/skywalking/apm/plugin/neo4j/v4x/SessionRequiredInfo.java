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

package org.apache.skywalking.apm.plugin.neo4j.v4x;

import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

/**
 * Cache session connection information, like database name and host and so on.
 */
public class SessionRequiredInfo {

    private AbstractSpan span;
    private ContextSnapshot contextSnapshot;

    public ContextSnapshot getContextSnapshot() {
        return contextSnapshot;
    }

    public void setContextSnapshot(ContextSnapshot contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }

    public AbstractSpan getSpan() {
        return span;
    }

    public void setSpan(AbstractSpan span) {
        this.span = span;
    }
}
