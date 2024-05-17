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

package org.apache.skywalking.apm.agent.core.context;

import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ids.NewDistributedTraceId;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LocalSpan;
import org.apache.skywalking.apm.agent.core.context.trace.NoopSpan;
import org.apache.skywalking.apm.agent.core.profile.ProfileStatusContext;
import org.apache.skywalking.apm.agent.core.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.core.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.core.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.core.test.tools.TracingSegmentRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert;

@RunWith(TracingSegmentRunner.class)
public class ContinuedContextTest {

    @SegmentStoragePoint
    private SegmentStorage tracingData;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @BeforeClass
    public static void beforeClass() {
        Config.Agent.KEEP_TRACING = true;
    }

    @AfterClass
    public static void afterClass() {
        Config.Agent.KEEP_TRACING = false;
        ServiceManager.INSTANCE.shutdown();
    }

    @Test
    public void testContinued() {

        NewDistributedTraceId distributedTraceId = new NewDistributedTraceId();
        ContextSnapshot snapshot = new ContextSnapshot(
                "1, 2, 3",
                1,
                distributedTraceId,
                "/for-test-continued",
                new CorrelationContext(),
                new ExtensionContext(),
                ProfileStatusContext.createWithNone()
        );

        AbstractSpan span = ContextManager.createLocalSpan("test-span");
        ContextManager.continued(snapshot);

        Assert.assertEquals(distributedTraceId.getId(), ContextManager.getGlobalTraceId());
        ContextManager.stopSpan();
    }

    @Test
    public void testContinuedWithIgnoredSnapshot() {

        ContextSnapshot snapshot =
                new ContextSnapshot(null, -1, null, null, new CorrelationContext(), new ExtensionContext(), ProfileStatusContext.createWithNone());

        AbstractSpan span = ContextManager.createLocalSpan("test-span");
        ContextManager.continued(snapshot);

        Assert.assertTrue(span instanceof LocalSpan);

        AbstractSpan span2 = ContextManager.createLocalSpan("test-span2");
        Assert.assertTrue(span2 instanceof NoopSpan);

        ContextManager.stopSpan();
        ContextManager.stopSpan();
    }

}
