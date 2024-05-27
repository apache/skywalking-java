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

package org.apache.skywalking.apm.plugin.elasticsearch.v7.support;

import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.plugin.elasticsearch.common.RestClientEnhanceInfo;
import org.elasticsearch.action.ActionListener;

public class RestClientCache<T> {
    private RestClientEnhanceInfo enhanceInfo;
    private ActionListener<T> actionListener;
    private ContextSnapshot contextSnapshot;
    private String operationName;

    public ContextSnapshot getContextSnapshot() {
        return contextSnapshot;
    }

    public void setContextSnapshot(final ContextSnapshot contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(final String operationName) {
        this.operationName = operationName;
    }

    public ActionListener<T> getActionListener() {
        return actionListener;
    }

    public void setActionListener(final ActionListener<T> actionListener) {
        this.actionListener = actionListener;
    }

    public RestClientEnhanceInfo getEnhanceInfo() {
        return enhanceInfo;
    }

    public void setEnhanceInfo(final RestClientEnhanceInfo enhanceInfo) {
        this.enhanceInfo = enhanceInfo;
    }
}
