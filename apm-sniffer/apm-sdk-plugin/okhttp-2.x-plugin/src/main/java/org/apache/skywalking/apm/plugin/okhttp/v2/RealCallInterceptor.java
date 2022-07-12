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

package org.apache.skywalking.apm.plugin.okhttp.v2;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * {@link RealCallInterceptor} intercept the synchronous http calls by the discovery of okhttp.
 */
public class RealCallInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    private static Field FIELD_HEADERS_OF_REQUEST;

    private static final ILog LOGGER = LogManager.getLogger(RealCallInterceptor.class);

    static {
        try {
            final Field field = Request.class.getDeclaredField("headers");
            field.setAccessible(true);
            FIELD_HEADERS_OF_REQUEST = field;
        } catch (Exception ignore) {
            FIELD_HEADERS_OF_REQUEST = null;
        }
    }

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        objInst.setSkyWalkingDynamicField(allArguments[1]);
    }

    /**
     * Get the {@link Request} from {@link EnhancedInstance}, then create {@link AbstractSpan} and set host,
     * port, kind, component, url from {@link Request}. Through the reflection of the way, set the http header
     * of context data into {@link Request#headers()}.
     *
     * @param result change this result, if you want to truncate the method.
     */
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            MethodInterceptResult result) throws Throwable {
        Request request = (Request) objInst.getSkyWalkingDynamicField();

        ContextCarrier contextCarrier = new ContextCarrier();
        HttpUrl requestUrl = request.httpUrl();
        AbstractSpan span = ContextManager.createExitSpan(requestUrl.uri().getPath(), contextCarrier,
                requestUrl.host() + ":" + requestUrl.port());
        ContextManager.inject(contextCarrier);
        span.setComponent(ComponentsDefine.OKHTTP);
        Tags.HTTP.METHOD.set(span, request.method());
        Tags.URL.set(span, requestUrl.uri().toString());
        SpanLayer.asHttp(span);

        if (FIELD_HEADERS_OF_REQUEST != null) {
            Headers.Builder headerBuilder = request.headers().newBuilder();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                headerBuilder.set(next.getHeadKey(), next.getHeadValue());
            }
            FIELD_HEADERS_OF_REQUEST.set(request, headerBuilder.build());
        }
    }

    /**
     * Get the status code from {@link Response}, when status code greater than 400, it means there was some errors in
     * the server. Finish the {@link AbstractSpan}.
     *
     * @param ret the method's original return value.
     */
    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            Object ret) throws Throwable {
        Response response = (Response) ret;
        if (response != null) {
            int statusCode = response.code();
            AbstractSpan span = ContextManager.activeSpan();
            Tags.HTTP_RESPONSE_STATUS_CODE.set(span, statusCode);
            if (statusCode >= 400) {
                span.errorOccurred();
            }
        }

        ContextManager.stopSpan();

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan abstractSpan = ContextManager.activeSpan();
        abstractSpan.log(t);
    }
}
