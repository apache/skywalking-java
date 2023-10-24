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

package org.apache.skywalking.apm.plugin.nacos.v2;

import com.alibaba.nacos.api.config.remote.request.ConfigChangeNotifyRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigPublishRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigRemoveRequest;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.api.naming.remote.request.InstanceRequest;
import com.alibaba.nacos.api.naming.remote.request.NotifySubscriberRequest;
import com.alibaba.nacos.api.naming.remote.request.ServiceListRequest;
import com.alibaba.nacos.api.naming.remote.request.ServiceQueryRequest;
import com.alibaba.nacos.api.naming.remote.request.SubscribeServiceRequest;
import com.alibaba.nacos.api.remote.request.Request;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.nacos.v2.constant.NacosConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("rawtypes")
public class NacosRequestOpt {

    private static final Map<Class, Handler> OPTS = new HashMap<>();

    static {
        OPTS.put(InstanceRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosExitSpan(peer);
            span.setOperationName(span.getOperationName() + ((InstanceRequest) request).getType());
            span.tag(NacosConstants.NAME_SPACE_TAG, ((InstanceRequest) request).getNamespace());
            span.tag(NacosConstants.GROUP_TAG, ((InstanceRequest) request).getGroupName());
            span.tag(NacosConstants.SERVICE_NAME_TAG, ((InstanceRequest) request).getServiceName());
        });

        OPTS.put(ServiceQueryRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosExitSpan(peer);
            span.setOperationName(span.getOperationName() + NacosConstants.QUERY_SERVICE);
            span.tag(NacosConstants.NAME_SPACE_TAG, ((ServiceQueryRequest) request).getNamespace());
            span.tag(NacosConstants.GROUP_TAG, ((ServiceQueryRequest) request).getGroupName());
            span.tag(NacosConstants.SERVICE_NAME_TAG, ((ServiceQueryRequest) request).getServiceName());
        });

        OPTS.put(SubscribeServiceRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosExitSpan(peer);
            span.setOperationName(span.getOperationName() + (((SubscribeServiceRequest) request).isSubscribe() ?
                    NacosConstants.SUBSCRIBE_SERVICE : NacosConstants.UNSUBSCRIBE_SERVICE));
            span.tag(NacosConstants.NAME_SPACE_TAG, ((SubscribeServiceRequest) request).getNamespace());
            span.tag(NacosConstants.GROUP_TAG, ((SubscribeServiceRequest) request).getGroupName());
            span.tag(NacosConstants.SERVICE_NAME_TAG, ((SubscribeServiceRequest) request).getServiceName());
        });

        OPTS.put(ServiceListRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosExitSpan(peer);
            span.setOperationName(span.getOperationName() + NacosConstants.GET_SERVICE_LIST);
            span.tag(NacosConstants.NAME_SPACE_TAG, ((ServiceListRequest) request).getNamespace());
            span.tag(NacosConstants.GROUP_TAG, ((ServiceListRequest) request).getGroupName());
            span.tag(NacosConstants.SERVICE_NAME_TAG, ((ServiceListRequest) request).getServiceName());
        });

        OPTS.put(ConfigQueryRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosExitSpan(peer);
            span.setOperationName(span.getOperationName() + NacosConstants.QUERY_CONFIG);
            span.tag(NacosConstants.DATA_ID_TAG, ((ConfigQueryRequest) request).getDataId());
            span.tag(NacosConstants.GROUP_TAG, ((ConfigQueryRequest) request).getGroup());
            span.tag(NacosConstants.TENANT_TAG, ((ConfigQueryRequest) request).getTenant());
        });

        OPTS.put(ConfigPublishRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosExitSpan(peer);
            span.setOperationName(span.getOperationName() + NacosConstants.PUBLISH_CONFIG);
            span.tag(NacosConstants.DATA_ID_TAG, ((ConfigPublishRequest) request).getDataId());
            span.tag(NacosConstants.GROUP_TAG, ((ConfigPublishRequest) request).getGroup());
            span.tag(NacosConstants.TENANT_TAG, ((ConfigPublishRequest) request).getTenant());
        });

        OPTS.put(ConfigRemoveRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosExitSpan(peer);
            span.setOperationName(span.getOperationName() + NacosConstants.REMOVE_CONFIG);
            span.tag(NacosConstants.DATA_ID_TAG, ((ConfigRemoveRequest) request).getDataId());
            span.tag(NacosConstants.GROUP_TAG, ((ConfigRemoveRequest) request).getGroup());
            span.tag(NacosConstants.TENANT_TAG, ((ConfigRemoveRequest) request).getTenant());
        });

        OPTS.put(NotifySubscriberRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosEntrySpan(peer);
            span.setOperationName(span.getOperationName() + NacosConstants.NOTIFY_SUBSCRIBE_CHANGE);
            ServiceInfo serviceInfo = ((NotifySubscriberRequest) request).getServiceInfo();
            span.tag(NacosConstants.GROUP_TAG, serviceInfo.getGroupName());
            span.tag(NacosConstants.SERVICE_NAME_TAG, serviceInfo.getName());
        });

        OPTS.put(ConfigChangeNotifyRequest.class, (request, peer) -> {
            AbstractSpan span = getNacosEntrySpan(peer);
            span.setOperationName(span.getOperationName() + NacosConstants.NOTIFY_CONFIG_CHANGE);
            span.tag(NacosConstants.DATA_ID_TAG, ((ConfigChangeNotifyRequest) request).getDataId());
            span.tag(NacosConstants.GROUP_TAG, ((ConfigChangeNotifyRequest) request).getGroup());
            span.tag(NacosConstants.TENANT_TAG, ((ConfigChangeNotifyRequest) request).getTenant());
        });
    }

    private static AbstractSpan getNacosEntrySpan(String peer) {
        AbstractSpan span = ContextManager.createEntrySpan(NacosConstants.NACOS_PREFIX, null);
        span.setComponent(ComponentsDefine.NACOS);
        span.setPeer(peer);
        SpanLayer.asRPCFramework(span);
        return span;
    }

    private static AbstractSpan getNacosExitSpan(String peer) {
        AbstractSpan span = ContextManager.createExitSpan(NacosConstants.NACOS_PREFIX, peer);
        span.setComponent(ComponentsDefine.NACOS);
        SpanLayer.asRPCFramework(span);
        return span;
    }

    public static Optional<Handler> getHandler(Class clazz) {
        return Optional.ofNullable(OPTS.get(clazz));
    }

    interface Handler {
        void buildRequestSpanInfo(Request request, String peer);
    }
}
