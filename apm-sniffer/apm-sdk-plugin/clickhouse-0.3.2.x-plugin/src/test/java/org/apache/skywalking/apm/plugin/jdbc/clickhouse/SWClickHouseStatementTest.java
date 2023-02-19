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

import com.clickhouse.jdbc.internal.ClickHouseConnectionImpl;
import com.clickhouse.jdbc.internal.ClickHouseStatementImpl;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class SWClickHouseStatementTest extends AbstractStatementTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ClickHouseStatementImpl clickHouseStatement;
    @Mock
    private ClickHouseConnectionImpl jdbcConnection;
    private ConnectionInfo connectionInfo;
    SWClickhouseConnection swClickhouseConnection;

    @Before
    public void setUp() throws Exception {
        connectionInfo = new ConnectionInfo(ComponentsDefine.CLICKHOUSE_JDBC_DRIVER, "ClickHouse", "127.0.0.1", 8123, "test");
        swClickhouseConnection = new SWClickhouseConnection("jdbc:clickhouse://127.0.0.1:8123/test", new Properties(), jdbcConnection, connectionInfo);
        when(jdbcConnection.createStatement()).thenReturn(clickHouseStatement);
        when(jdbcConnection.createStatement(anyInt(), anyInt())).thenReturn(clickHouseStatement);
        when(jdbcConnection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(clickHouseStatement);
    }

    @Test
    public void testPreparedStatementConfig() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement();
        statement.cancel();
        statement.getUpdateCount();
        statement.setFetchDirection(1);
        statement.getFetchDirection();
        statement.getResultSetConcurrency();
        statement.getResultSetType();
        statement.isClosed();
        statement.setPoolable(false);
        statement.isPoolable();
        statement.getWarnings();
        statement.clearWarnings();
        statement.setCursorName("test");
        statement.setMaxFieldSize(11);
        statement.getMaxFieldSize();
        statement.setMaxRows(10);
        statement.getMaxRows();
        statement.setEscapeProcessing(true);
        statement.setFetchSize(1);
        statement.getFetchSize();
        statement.setQueryTimeout(1);
        statement.getQueryTimeout();
        Connection connection = statement.getConnection();

        statement.execute("SELECT * FROM test");
        statement.getMoreResults();
        statement.getMoreResults(1);
        statement.getResultSetHoldability();
        statement.getResultSet();

        statement.close();
        verify(clickHouseStatement).getUpdateCount();
        verify(clickHouseStatement).getMoreResults();
        verify(clickHouseStatement).setFetchDirection(anyInt());
        verify(clickHouseStatement).getFetchDirection();
        verify(clickHouseStatement).getResultSetType();
        verify(clickHouseStatement).isClosed();
        verify(clickHouseStatement).setPoolable(anyBoolean());
        verify(clickHouseStatement).getWarnings();
        verify(clickHouseStatement).clearWarnings();
        verify(clickHouseStatement).setCursorName(anyString());
        verify(clickHouseStatement).setMaxFieldSize(anyInt());
        verify(clickHouseStatement).getMaxFieldSize();
        verify(clickHouseStatement).setMaxRows(anyInt());
        verify(clickHouseStatement).getMaxRows();
        verify(clickHouseStatement).setEscapeProcessing(anyBoolean());
        verify(clickHouseStatement).getResultSetConcurrency();
        verify(clickHouseStatement).getResultSetConcurrency();
        verify(clickHouseStatement).getResultSetType();
        verify(clickHouseStatement).getMoreResults(anyInt());
        verify(clickHouseStatement).setFetchSize(anyInt());
        verify(clickHouseStatement).getFetchSize();
        verify(clickHouseStatement).getQueryTimeout();
        verify(clickHouseStatement).setQueryTimeout(anyInt());
        verify(clickHouseStatement).getResultSet();
        assertThat(connection, CoreMatchers.<Connection>is(swClickhouseConnection));

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/execute", "SELECT * FROM test");
    }

    @Test
    public void testExecuteWithAutoGeneratedKey() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1);
        boolean executeSuccess = statement.execute("SELECT * FROM test", 1);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/execute", "SELECT * FROM test");

    }

    @Test
    public void testExecuteQuery() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1, 1);
        ResultSet executeSuccess = statement.executeQuery("SELECT * FROM test");

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/executeQuery", "SELECT * FROM test");

    }

    @Test
    public void testExecuteUpdate() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1, 1);
        int executeSuccess = statement.executeUpdate("UPDATE test SET a = 1");

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/executeUpdate", "UPDATE test SET a = 1");

    }

    @Test
    public void testExecuteUpdateWithAutoGeneratedKey() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1, 1);
        int executeSuccess = statement.executeUpdate("UPDATE test SET a = 1", 1);
        statement.getGeneratedKeys();

        verify(clickHouseStatement).getGeneratedKeys();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/executeUpdate", "UPDATE test SET a = 1");

    }

    @Test
    public void testExecuteUpdateWithColumnIndexes() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1, 1);
        int executeSuccess = statement.executeUpdate("UPDATE test SET a = 1", new int[]{1});

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/executeUpdate", "UPDATE test SET a = 1");

    }

    @Test
    public void testExecuteUpdateWithColumnStringIndexes() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1, 1);
        int executeSuccess = statement.executeUpdate("UPDATE test SET a = 1", new String[]{"1"});

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/executeUpdate", "UPDATE test SET a = 1");

    }

    @Test
    public void testExecuteWithColumnIndexes() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1, 1);
        boolean executeSuccess = statement.execute("UPDATE test SET a = 1", new int[]{1});

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/execute", "UPDATE test SET a = 1");

    }

    @Test
    public void testExecuteWithColumnStringIndexes() throws SQLException {
        Statement statement = swClickhouseConnection.createStatement(1, 1, 1);
        boolean executeSuccess = statement.execute("UPDATE test SET a = 1", new String[]{"1"});

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/execute", "UPDATE test SET a = 1");
    }

    @Test
    public void testBatch() throws SQLException, MalformedURLException {
        Statement statement = swClickhouseConnection.createStatement();
        statement.addBatch("UPDATE test SET a = 1 WHERE b = 2");
        int[] resultSet = statement.executeBatch();
        statement.clearBatch();

        verify(clickHouseStatement).executeBatch();
        verify(clickHouseStatement).addBatch(anyString());
        verify(clickHouseStatement).clearBatch();

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/Statement/executeBatch", "");

    }

}
