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

package org.apache.skywalking.apm.plugin.sofarpc;

import com.alipay.remoting.InvokeCallback;
import java.util.concurrent.Executor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SofaBoltCallbackInvokeInterceptorTest {
    InvokeCallback callback;
    Object obj;
    Object[] matchArgs;
    Object[] mismatchArgs;

    @Before
    public void before() {
        callback = new InvokeCallback() {
            @Override
            public void onResponse(final Object o) {

            }

            @Override
            public void onException(final Throwable throwable) {

            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        };

        obj = new Object();

        matchArgs = new Object[] {
            null,
            null,
            callback,
            null
        };
        mismatchArgs = new Object[] {
            null,
            null,
            obj,
            null
        };
    }

    @Test
    public void testOverrideArguments() {
        final SofaBoltCallbackInvokeInterceptor interceptor = new SofaBoltCallbackInvokeInterceptor();
        interceptor.beforeMethod(null, null, matchArgs, null, null);
        Assert.assertTrue(matchArgs[2] instanceof InvokeCallbackWrapper);
        Assert.assertSame(callback, ((InvokeCallbackWrapper) matchArgs[2]).getInvokeCallback());

        interceptor.beforeMethod(null, null, mismatchArgs, null, null);
        Assert.assertSame(obj, mismatchArgs[2]);
    }

}