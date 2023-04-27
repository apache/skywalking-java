/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.sms.tencent;

import com.tencentcloudapi.sms.v20190711.SmsClient;
import com.tencentcloudapi.sms.v20190711.models.SendSmsResponse;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TencentSmsInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String TENCENT_SMS = "Tencent SMS";
    private static final String TAG_SEND_SUCCESS_COUNT = "send_success_count";
    private static final String TAG_SEND_FAIL_COUNT = "send_error_count";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult context) throws Throwable {
        final ContextCarrier contextCarrier = new ContextCarrier();
        SmsClient sms = (SmsClient) objInst;
        String endpoint = sms.getClientProfile().getHttpProfile().getEndpoint();
        AbstractSpan span = ContextManager.createExitSpan(TENCENT_SMS, contextCarrier, endpoint);
        Tags.URL.set(span, endpoint);
        span.setComponent(ComponentsDefine.TENCENT_SMS);
        SpanLayer.asHttp(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (ret != null) {
            if (ret instanceof SendSmsResponse) {
                SendSmsResponse sendSmsResponse = (SendSmsResponse) ret;
                long sendSuccessCount = Arrays.stream(sendSmsResponse.getSendStatusSet())
                        .filter(sendStatus -> "OK".equalsIgnoreCase(sendStatus.getCode()) && sendStatus.getFee() > 0).count();

                AbstractSpan span = ContextManager.activeSpan();
                StringTag successCountTag = new StringTag(TAG_SEND_SUCCESS_COUNT);
                StringTag failCountTag = new StringTag(TAG_SEND_FAIL_COUNT);
                span.tag(successCountTag, String.valueOf(sendSuccessCount));
                span.tag(failCountTag, String.valueOf(sendSmsResponse.getSendStatusSet().length - sendSuccessCount));
            }
        }
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
