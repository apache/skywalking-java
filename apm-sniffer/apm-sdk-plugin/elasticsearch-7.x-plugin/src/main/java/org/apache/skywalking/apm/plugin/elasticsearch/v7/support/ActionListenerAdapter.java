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

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.elasticsearch.v7.Constants;
import org.elasticsearch.action.ActionListener;

import static org.apache.skywalking.apm.plugin.elasticsearch.v7.Constants.DB_TYPE;

public class ActionListenerAdapter<T> implements ActionListener<T>, EnhancedInstance {

    private final RestClientCache<T> restClientCache;

    public ActionListenerAdapter(RestClientCache<T> restClientCache) {
        this.restClientCache = restClientCache;
    }

    @Override
    public void onResponse(final T o) {
        try {
            if (restClientCache.getContextSnapshot() != null) {
                continueContext(Constants.ON_RESPONSE_SUFFIX);
            }
            if (restClientCache.getActionListener() != null) {
                restClientCache.getActionListener().onResponse(o);
            }
        } catch (Throwable t) {
            ContextManager.activeSpan().log(t);
            throw t;
        } finally {
            restClientCache.setContextSnapshot(null);
            ContextManager.stopSpan();
        }
    }

    @Override
    public void onFailure(final Exception e) {
        try {
            if (restClientCache.getContextSnapshot() != null) {
                continueContext(Constants.ON_FAILURE_SUFFIX);
            }
            if (restClientCache.getActionListener() != null) {
                restClientCache.getActionListener().onFailure(e);
            }
        } catch (Throwable t) {
            ContextManager.activeSpan().log(t);
            throw t;
        } finally {
            ContextManager.stopSpan();
        }
    }

    @Override
    public Object getSkyWalkingDynamicField() {
        return restClientCache;
    }

    @Override
    public void setSkyWalkingDynamicField(final Object enhanceInfo) {

    }

    private void continueContext(String action) {
        AbstractSpan activeSpan = ContextManager.createLocalSpan(restClientCache.getOperationName() + action);
        activeSpan.setComponent(ComponentsDefine.REST_HIGH_LEVEL_CLIENT);
        Tags.DB_TYPE.set(activeSpan, DB_TYPE);
        SpanLayer.asDB(activeSpan);
        ContextManager.continued(restClientCache.getContextSnapshot());
    }
}
