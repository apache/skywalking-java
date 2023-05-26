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

package org.apache.skywalking.apm.plugin.jdbc.mariadb.v2;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
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
import org.apache.skywalking.apm.plugin.jdbc.JDBCPluginConfig;
import org.apache.skywalking.apm.plugin.jdbc.define.StatementEnhanceInfos;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class StatementExecuteMethodsInterceptorTest {

    private static final String SQL = "Select * from test";

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private StatementExecuteMethodsInterceptor serviceMethodInterceptor;

    @Mock
    private ConnectionInfo connectionInfo;
    @Mock
    private EnhancedInstance objectInstance;
    @Mock
    private Method method;
    private StatementEnhanceInfos enhanceRequireCacheObject;

    @Before
    public void setUp() {
        JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH = 2048;

        serviceMethodInterceptor = new StatementExecuteMethodsInterceptor();

        enhanceRequireCacheObject = new StatementEnhanceInfos(connectionInfo, SQL, "CallableStatement");
        when(objectInstance.getSkyWalkingDynamicField()).thenReturn(enhanceRequireCacheObject);
        when(method.getName()).thenReturn("executeQuery");
        when(connectionInfo.getComponent()).thenReturn(ComponentsDefine.MARIADB_JDBC);
        when(connectionInfo.getDBType()).thenReturn("Mariadb");
        when(connectionInfo.getDatabaseName()).thenReturn("test");
        when(connectionInfo.getDatabasePeer()).thenReturn("localhost:3306");
    }

    @Test
    public void testExecuteStatement() {
        serviceMethodInterceptor.beforeMethod(objectInstance, method, new Object[]{SQL}, null, null);
        serviceMethodInterceptor.afterMethod(objectInstance, method, new Object[]{SQL}, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        assertThat(SegmentHelper.getSpans(segment).size(), is(1));
        AbstractTracingSpan span = SegmentHelper.getSpans(segment).get(0);
        SpanAssert.assertLayer(span, SpanLayer.DB);
        assertThat(span.getOperationName(), is("Mariadb/JDBC/CallableStatement/executeQuery"));
        SpanAssert.assertTag(span, 0, "Mariadb");
        SpanAssert.assertTag(span, 1, "test");
        SpanAssert.assertTag(span, 2, SQL);
    }

    @Test
    public void testExecuteStatementWithLimitSqlBody() {
        JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH = 10;
        serviceMethodInterceptor.beforeMethod(objectInstance, method, new Object[]{SQL}, null, null);
        serviceMethodInterceptor.afterMethod(objectInstance, method, new Object[]{SQL}, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        assertThat(SegmentHelper.getSpans(segment).size(), is(1));
        AbstractTracingSpan span = SegmentHelper.getSpans(segment).get(0);
        SpanAssert.assertLayer(span, SpanLayer.DB);
        assertThat(span.getOperationName(), is("Mariadb/JDBC/CallableStatement/executeQuery"));
        SpanAssert.assertTag(span, 0, "Mariadb");
        SpanAssert.assertTag(span, 1, "test");
        SpanAssert.assertTag(span, 2, "Select * f...");
    }

}
