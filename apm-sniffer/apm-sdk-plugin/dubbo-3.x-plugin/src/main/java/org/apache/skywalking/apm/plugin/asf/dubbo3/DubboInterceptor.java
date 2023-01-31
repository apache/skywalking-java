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

package org.apache.skywalking.apm.plugin.asf.dubbo3;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

import java.lang.reflect.Method;

/**
 * {@link DubboInterceptor} define how to enhance class {@link org.apache.dubbo.monitor.support.MonitorFilter#invoke(Invoker,
 * Invocation)}. the trace context transport to the provider side by {@link RpcContext#getServerAttachment}.but all the
 * version of dubbo framework below 3.0.0 don't support {@link RpcContext#getClientAttachment}, we support another way
 * to support it.
 */
public class DubboInterceptor implements InstanceMethodsAroundInterceptor {

    public static final String ARGUMENTS = "arguments";

    /**
     * Consumer: The serialized trace context data will
     * inject to the {@link RpcContext#getClientAttachment} for transport to provider side.
     * <p>
     * Provider: The serialized trace context data will extract from
     * {@link RpcContext#getServerAttachment}. current trace segment will ref if the serialization context data is not
     * null.
     */
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        Invoker invoker = (Invoker) allArguments[0];
        Invocation invocation = (Invocation) allArguments[1];

        boolean isConsumer = isConsumer(invocation);

        RpcContextAttachment attachment = isConsumer ? RpcContext.getClientAttachment() : RpcContext.getServerAttachment();
        URL requestURL = invoker.getUrl();

        AbstractSpan span;

        final String host = requestURL.getHost();
        final int port = requestURL.getPort();

        boolean needCollectArguments;
        int argumentsLengthThreshold;
        if (isConsumer) {
            final ContextCarrier contextCarrier = new ContextCarrier();
            span = ContextManager.createExitSpan(
                generateOperationName(requestURL, invocation), contextCarrier, host + ":" + port);
            //invocation.getAttachments().put("contextData", contextDataStr);
            //@see https://github.com/alibaba/dubbo/blob/dubbo-2.5.3/dubbo-rpc/dubbo-rpc-api/src/main/java/com/alibaba/dubbo/rpc/RpcInvocation.java#L154-L161
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                attachment.setAttachment(next.getHeadKey(), next.getHeadValue());
                if (invocation.getAttachments().containsKey(next.getHeadKey())) {
                    invocation.getAttachments().remove(next.getHeadKey());
                }
            }
            needCollectArguments = DubboPluginConfig.Plugin.Dubbo.COLLECT_CONSUMER_ARGUMENTS;
            argumentsLengthThreshold = DubboPluginConfig.Plugin.Dubbo.CONSUMER_ARGUMENTS_LENGTH_THRESHOLD;
        } else {
            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                next.setHeadValue(attachment.getAttachment(next.getHeadKey()));
            }

            span = ContextManager.createEntrySpan(generateOperationName(requestURL, invocation), contextCarrier);
            span.setPeer(attachment.getRemoteAddressString());
            needCollectArguments = DubboPluginConfig.Plugin.Dubbo.COLLECT_PROVIDER_ARGUMENTS;
            argumentsLengthThreshold = DubboPluginConfig.Plugin.Dubbo.PROVIDER_ARGUMENTS_LENGTH_THRESHOLD;
        }

        Tags.URL.set(span, generateRequestURL(requestURL, invocation));
        collectArguments(needCollectArguments, argumentsLengthThreshold, span, invocation);
        span.setComponent(ComponentsDefine.DUBBO);
        SpanLayer.asRPCFramework(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        Result result = (Result) ret;
        if (result != null && result.getException() != null) {
            dealException(result.getException());
        }

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        dealException(t);
    }

    /**
     * Log the throwable, which occurs in Dubbo RPC service.
     */
    private void dealException(Throwable throwable) {
        AbstractSpan span = ContextManager.activeSpan();
        span.log(throwable);
    }

    /**
     * To judge if current is in provider side.
     */
    private static boolean isConsumer(Invocation invocation) {
        Invoker<?> invoker = invocation.getInvoker();
        // As RpcServiceContext may not been reset when it's role switched from provider
        // to consumer in the same thread, but RpcInvocation is always correctly bounded
        // to the current request or serve request, https://github.com/apache/skywalking-java/pull/110
        return invoker.getUrl()
                .getParameter("side", "provider")
                .equals("consumer");
    }

    /**
     * Format operation name. e.g. org.apache.skywalking.apm.plugin.test.Test.test(String)
     *
     * @return operation name.
     */
    private String generateOperationName(URL requestURL, Invocation invocation) {
        StringBuilder operationName = new StringBuilder();
        String groupStr = requestURL.getParameter(CommonConstants.GROUP_KEY);
        groupStr = StringUtil.isEmpty(groupStr) ? "" : groupStr + "/";
        operationName.append(groupStr);
        operationName.append(requestURL.getPath());
        operationName.append("." + invocation.getMethodName() + "(");
        for (Class<?> classes : invocation.getParameterTypes()) {
            operationName.append(classes.getSimpleName() + ",");
        }

        if (invocation.getParameterTypes().length > 0) {
            operationName.delete(operationName.length() - 1, operationName.length());
        }

        operationName.append(")");

        return operationName.toString();
    }

    /**
     * Format request url. e.g. dubbo://127.0.0.1:20880/org.apache.skywalking.apm.plugin.test.Test.test(String).
     *
     * @return request url.
     */
    private String generateRequestURL(URL url, Invocation invocation) {
        StringBuilder requestURL = new StringBuilder();
        requestURL.append(url.getProtocol() + "://");
        requestURL.append(url.getHost());
        requestURL.append(":" + url.getPort() + "/");
        requestURL.append(generateOperationName(url, invocation));
        return requestURL.toString();
    }

    private void collectArguments(boolean needCollectArguments,
                                  int argumentsLengthThreshold,
                                  AbstractSpan span,
                                  Invocation invocation) {
        if (needCollectArguments && argumentsLengthThreshold > 0) {
            Object[] parameters = invocation.getArguments();
            if (parameters != null && parameters.length > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                boolean first = true;
                for (Object parameter : parameters) {
                    if (!first) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(parameter);
                    if (stringBuilder.length() > argumentsLengthThreshold) {
                        stringBuilder.append("...");
                        break;
                    }
                    first = false;
                }
                span.tag(ARGUMENTS, stringBuilder.toString());
            }
        }
    }
}
