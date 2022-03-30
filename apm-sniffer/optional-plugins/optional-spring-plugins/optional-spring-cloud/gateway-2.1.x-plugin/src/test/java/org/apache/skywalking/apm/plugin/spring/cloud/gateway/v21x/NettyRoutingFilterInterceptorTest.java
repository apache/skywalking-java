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

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v21x;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class NettyRoutingFilterInterceptorTest {

    private static final String NETTY_ROUTING_FILTERED_ATTR =
            NettyRoutingFilterInterceptor.class.getName() + ".alreadyFiltered";

    private final NettyRoutingFilterInterceptor interceptor = new NettyRoutingFilterInterceptor();

    private final ServerWebExchange exchange = new DefaultServerWebExchange(null, null, null, null,
            null);

    @Test
    public void testAlreadyFiltered() throws Throwable {
        interceptor.beforeMethod(null, null, new Object[]{exchange}, null, null);
        interceptor.afterMethod(null, null, null, null, null);
        Assert.assertEquals(exchange.getAttributes().get(NETTY_ROUTING_FILTERED_ATTR), true);
        Assert.assertNotNull(ContextManager.activeSpan());

        ContextManager.stopSpan();

        interceptor.beforeMethod(null, null, new Object[]{exchange}, null, null);
        interceptor.afterMethod(null, null, null, null, null);
        Assert.assertEquals(exchange.getAttributes().get(NETTY_ROUTING_FILTERED_ATTR), true);
        Assert.assertNull(ContextManager.activeSpan());
    }
}
