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

package org.apache.skywalking.apm.plugin.jdbc.postgresql;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.sql.SQLException;
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
public class PreparedStatementExecuteMethodsInterceptorTest {

    private static final String SQL = "INSERT INTO test (name) VALUES (?)";

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private PreparedStatementExecuteMethodsInterceptor interceptor;

    @Mock
    private ConnectionInfo connectionInfo;
    @Mock
    private EnhancedInstance objectInstance;
    @Mock
    private Method method;

    @Before
    public void setUp() {
        JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH = 2048;
        interceptor = new PreparedStatementExecuteMethodsInterceptor();
    }

    @Test
    public void testCreateDatabaseSpan() throws Throwable {
        when(objectInstance.getSkyWalkingDynamicField()).thenReturn(
            new StatementEnhanceInfos(connectionInfo, SQL, "PreparedStatement"));
        when(method.getName()).thenReturn("executeUpdate");
        when(connectionInfo.getComponent()).thenReturn(ComponentsDefine.POSTGRESQL_DRIVER);
        when(connectionInfo.getDBType()).thenReturn("PostgreSQL");
        when(connectionInfo.getDatabaseName()).thenReturn("test");
        when(connectionInfo.getDatabasePeer()).thenReturn("localhost:5432");

        interceptor.beforeMethod(objectInstance, method, new Object[0], null, null);
        interceptor.afterMethod(objectInstance, method, new Object[0], null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        assertThat(SegmentHelper.getSpans(segment).size(), is(1));
        AbstractTracingSpan span = SegmentHelper.getSpans(segment).get(0);
        SpanAssert.assertLayer(span, SpanLayer.DB);
        assertThat(span.getOperationName(), is("PostgreSQL/JDBC/PreparedStatement/executeUpdate"));
        SpanAssert.assertTag(span, 0, "PostgreSQL");
        SpanAssert.assertTag(span, 1, "test");
        SpanAssert.assertTag(span, 2, SQL);
    }

    /**
     * A statement created by {@code PgConnection#prepareStatement(String, String[])}, which is also reached through
     * {@code prepareStatement(String, int)} with {@code RETURN_GENERATED_KEYS}, is wrapped by {@code SWPreparedStatement}
     * without setting the dynamic field, so the interceptor must skip it instead of throwing NPE.
     */
    @Test
    public void testSkipStatementWithoutEnhanceInfos() throws Throwable {
        when(objectInstance.getSkyWalkingDynamicField()).thenReturn(null);

        interceptor.beforeMethod(objectInstance, method, new Object[0], null, null);
        interceptor.handleMethodException(objectInstance, method, new Object[0], null, new SQLException());
        interceptor.afterMethod(objectInstance, method, new Object[0], null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(0));
    }
}
