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

package org.apache.skywalking.apm.plugin.jdbc.clickhouse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import ru.yandex.clickhouse.ClickHouseStatementImpl;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class ClickHouseStatementMethodInterceptorTest {

    private final static String SQL = "SELECT 1";
    private final EnhancedInstance enhancedInstance = new EnhancedInstance() {
        private Object value;

        @Override
        public Object getSkyWalkingDynamicField() {
            return value;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.value = value;
        }
    };
    private final ClickHouseStatementMethodInterceptor interceptor = new ClickHouseStatementMethodInterceptor();
    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    @Mock
    private ConnectionInfo connectionInfo;
    @Mock
    private ClickHouseStatementImpl clickHouseStatement;

    @Before
    public void setUp() throws Exception {
        // Mock connection info instance method
        when(connectionInfo.getComponent()).thenReturn(ComponentsDefine.CLICKHOUSE_JDBC_DRIVER);
        when(connectionInfo.getDatabaseName()).thenReturn("default");
        when(connectionInfo.getDatabasePeer()).thenReturn("127.0.0.1:8123");
        when(connectionInfo.getDBType()).thenReturn("ClickHouse");

        // Mock clickhouse statement instance method
        when(clickHouseStatement.execute(SQL)).thenReturn(true);
    }

    @Test
    public void testWithoutConnectionInfo() throws Throwable {
        final Object ret = interceptor.afterMethod(enhancedInstance, null, new Object[0], new Class[0],
                clickHouseStatement);

        assertSame(clickHouseStatement, ret);
    }

    @Test
    public void test() throws Throwable {
        enhancedInstance.setSkyWalkingDynamicField(connectionInfo);
        final Object ret = interceptor.afterMethod(enhancedInstance, null, new Object[0], new Class[0],
                clickHouseStatement);

        assertNotSame(clickHouseStatement, ret);
        assertSame(TracedClickHouseStatement.class, ret.getClass());
        TracedClickHouseStatement statement = (TracedClickHouseStatement) ret;
        final boolean result = statement.execute(SQL);
        assertTrue(result);

        final List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(1));
        final List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegments.get(0));
        assertNotNull(spans);
        assertThat(spans.size(), is(1));
        assertSpan(spans.get(0));
    }

    private void assertSpan(final AbstractTracingSpan span) throws JsonProcessingException {
        SpanAssert.assertComponent(span, ComponentsDefine.CLICKHOUSE_JDBC_DRIVER);
        SpanAssert.assertLayer(span, SpanLayer.DB);
        SpanAssert.assertTagSize(span, 3);
        SpanAssert.assertTag(span, 0, "ClickHouse");
        SpanAssert.assertTag(span, 1, "default");
        SpanAssert.assertTag(span, 2, SQL);
    }
}