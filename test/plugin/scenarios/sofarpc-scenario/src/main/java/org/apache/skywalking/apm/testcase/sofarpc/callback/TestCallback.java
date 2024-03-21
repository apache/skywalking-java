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

package org.apache.skywalking.apm.testcase.sofarpc.callback;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import org.apache.skywalking.apm.testcase.sofarpc.interfaces.SofaRpcDemoService;

public class TestCallback implements SofaResponseCallback {

    private SofaRpcDemoService service;

    public TestCallback(final SofaRpcDemoService service) {
        this.service = service;
    }

    @Override
    public void onAppResponse(final Object o, final String s, final RequestBase requestBase) {
    }

    @Override
    public void onAppException(final Throwable throwable, final String s, final RequestBase requestBase) {

    }

    @Override
    public void onSofaException(final SofaRpcException e, final String s, final RequestBase requestBase) {

    }
}
