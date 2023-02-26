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
 */

package org.apache.skywalking.apm.plugin.armeria;

import com.linecorp.armeria.client.UserClient;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;

import java.net.URI;

public class Armeria100ClientInterceptor extends AbstractArmeriaClientInterceptor {

    @Override
    protected URI getUri(EnhancedInstance objInst) {
        UserClient userClient = (UserClient) objInst;
        return userClient.uri();
    }

    @Override
    protected HttpMethod getHttpMethod(Object[] allArguments) {
        return (HttpMethod) allArguments[2];
    }

    @Override
    protected String getPath(Object[] allArguments) {
        return (String) allArguments[3];
    }

    @Override
    protected HttpRequest getHttpRequest(Object[] allArguments) {
        return (HttpRequest) allArguments[6];
    }

}
