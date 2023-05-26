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

import com.clickhouse.jdbc.ClickHousePreparedStatement;
import com.clickhouse.jdbc.internal.ClickHouseConnectionImpl;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class SWClickHousePreparedStatementTest extends AbstractStatementTest {
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Array array;
    @Mock
    private SQLXML sqlxml;
    @Mock
    private RowId rowId;
    @Mock
    private Ref ref;
    @Mock
    private Clob clob;
    @Mock
    private NClob nClob;
    @Mock
    private Reader reader;
    @Mock
    private InputStream inputStream;
    @Mock
    private Blob blob;
    @Mock
    private ClickHousePreparedStatement clickHousePreparedStatement;
    @Mock
    private ClickHouseConnectionImpl jdbcConnection;
    private ConnectionInfo connectionInfo;
    private SWClickhouseConnection swClickhouseConnection;
    private byte[] bytesParam = new byte[]{1, 2};

    @Before
    public void setUp() throws Exception {
        connectionInfo = new ConnectionInfo(ComponentsDefine.CLICKHOUSE_JDBC_DRIVER, "ClickHouse", "127.0.0.1", 8123, "test");
        swClickhouseConnection = new SWClickhouseConnection("jdbc:clickhouse://127.0.0.1:8123/test", new Properties(), jdbcConnection, connectionInfo);
        when(jdbcConnection.prepareStatement(anyString())).thenReturn(clickHousePreparedStatement);
        when(jdbcConnection.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(clickHousePreparedStatement);
        when(jdbcConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(clickHousePreparedStatement);
        when(jdbcConnection.prepareStatement(anyString(), anyInt())).thenReturn(clickHousePreparedStatement);
    }

    @Test
    public void testSetParam() throws SQLException, MalformedURLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("SELECT * FROM test WHERE a = ? or b = ? or c=? or d = ? or e = ?" + " or e = ? or f = ? or g = ? or h = ? or i = ? or j = ? or k = ? or l = ? or m = ?  or n = ? or o = ? or p = ? " + " or r = ?  or s = ? or t = ?  or u = ?  or v = ?  or w = ?  or x = ?  or y = ? or z = ? or a1 = ? or a2 = ? or a3 = ?" + " or a4 = ? or a5 = ? or a6 = ?  or a7 = ?  or a8 = ?  or a9 = ? or b1 = ? or b2 = ? or b3 = ? or b4 = ? or b5 = ?" + " or b6 = ? or b7 = ? or b8  = ? or b9 = ? or c1 = ?  or c2 = ? or c3 = ?");
        preparedStatement.clearParameters();
        preparedStatement.setAsciiStream(1, inputStream);
        preparedStatement.setAsciiStream(2, inputStream, 10);
        preparedStatement.setAsciiStream(3, inputStream, 1000000L);
        preparedStatement.setCharacterStream(4, reader);
        preparedStatement.setCharacterStream(4, reader, 10);
        preparedStatement.setCharacterStream(5, reader, 10L);
        preparedStatement.setShort(6, (short) 12);
        preparedStatement.setInt(7, 1);
        preparedStatement.setString(8, "test");
        preparedStatement.setBoolean(9, true);
        preparedStatement.setLong(10, 100L);
        preparedStatement.setDouble(11, 12.0);
        preparedStatement.setFloat(12, 12.0f);
        preparedStatement.setByte(13, (byte) 1);
        preparedStatement.setBytes(14, bytesParam);
        preparedStatement.setDate(15, new Date(System.currentTimeMillis()));
        preparedStatement.setNull(16, 1);
        preparedStatement.setNull(17, 1, "test");
        preparedStatement.setBigDecimal(18, new BigDecimal(10000));
        preparedStatement.setBlob(19, inputStream);
        preparedStatement.setBlob(20, inputStream, 1000000L);
        preparedStatement.setClob(21, clob);
        preparedStatement.setClob(22, reader);
        preparedStatement.setClob(23, reader, 100L);
        preparedStatement.setNString(24, "test");
        preparedStatement.setNCharacterStream(25, reader);
        preparedStatement.setNCharacterStream(26, reader, 1);
        preparedStatement.setNClob(27, nClob);
        preparedStatement.setNClob(28, reader, 1);
        preparedStatement.setObject(29, new Object());
        preparedStatement.setObject(30, new Object(), 1);
        preparedStatement.setObject(31, new Object(), 1, 1);
        preparedStatement.setRef(32, ref);
        preparedStatement.setRowId(33, rowId);
        preparedStatement.setSQLXML(34, sqlxml);
        preparedStatement.setTime(35, new Time(System.currentTimeMillis()));
        preparedStatement.setTimestamp(36, new Timestamp(System.currentTimeMillis()));
        preparedStatement.setTimestamp(37, new Timestamp(System.currentTimeMillis()), Calendar.getInstance());
        preparedStatement.setURL(38, new URL("http", "127.0.0.1", "test"));
        preparedStatement.setBinaryStream(39, inputStream);
        preparedStatement.setBinaryStream(40, inputStream, 1);
        preparedStatement.setBinaryStream(41, inputStream, 1L);
        preparedStatement.setNClob(42, reader);
        preparedStatement.setTime(43, new Time(System.currentTimeMillis()), Calendar.getInstance());
        preparedStatement.setArray(45, array);
        preparedStatement.setBlob(46, blob);
        preparedStatement.setDate(47, new Date(System.currentTimeMillis()), Calendar.getInstance());

        ResultSet resultSet = preparedStatement.executeQuery();
        preparedStatement.close();

        verify(clickHousePreparedStatement).clearParameters();
        verify(clickHousePreparedStatement).executeQuery();
        verify(clickHousePreparedStatement).close();
        verify(clickHousePreparedStatement).setAsciiStream(anyInt(), any(InputStream.class));
        verify(clickHousePreparedStatement).setAsciiStream(anyInt(), any(InputStream.class), anyInt());
        verify(clickHousePreparedStatement).setAsciiStream(anyInt(), any(InputStream.class), anyLong());
        verify(clickHousePreparedStatement).setCharacterStream(anyInt(), any(Reader.class));
        verify(clickHousePreparedStatement).setCharacterStream(anyInt(), any(Reader.class), anyInt());
        verify(clickHousePreparedStatement).setCharacterStream(anyInt(), any(Reader.class), anyLong());
        verify(clickHousePreparedStatement).setShort(anyInt(), anyShort());
        verify(clickHousePreparedStatement).setInt(anyInt(), anyInt());
        verify(clickHousePreparedStatement).setString(anyInt(), anyString());
        verify(clickHousePreparedStatement).setBoolean(anyInt(), anyBoolean());
        verify(clickHousePreparedStatement).setLong(anyInt(), anyLong());
        verify(clickHousePreparedStatement).setDouble(anyInt(), anyDouble());
        verify(clickHousePreparedStatement).setFloat(anyInt(), anyFloat());
        verify(clickHousePreparedStatement).setByte(anyInt(), anyByte());
        verify(clickHousePreparedStatement).setBytes(14, bytesParam);
        verify(clickHousePreparedStatement).setDate(anyInt(), any(Date.class));
        verify(clickHousePreparedStatement).setNull(anyInt(), anyInt());
        verify(clickHousePreparedStatement).setNull(anyInt(), anyInt(), anyString());
        verify(clickHousePreparedStatement).setBigDecimal(anyInt(), any(BigDecimal.class));
        verify(clickHousePreparedStatement).setBlob(anyInt(), any(InputStream.class));
        verify(clickHousePreparedStatement).setBlob(anyInt(), any(InputStream.class), anyLong());
        verify(clickHousePreparedStatement).setClob(anyInt(), any(Clob.class));
        verify(clickHousePreparedStatement).setClob(anyInt(), any(Reader.class));
        verify(clickHousePreparedStatement).setClob(anyInt(), any(Reader.class), anyLong());
        verify(clickHousePreparedStatement).setNString(anyInt(), anyString());
        verify(clickHousePreparedStatement).setNCharacterStream(anyInt(), any(Reader.class));
        verify(clickHousePreparedStatement).setNCharacterStream(anyInt(), any(Reader.class), anyLong());
        verify(clickHousePreparedStatement).setNClob(27, nClob);
        verify(clickHousePreparedStatement).setNClob(28, reader, 1);
        verify(clickHousePreparedStatement).setObject(anyInt(), any());
        verify(clickHousePreparedStatement).setObject(anyInt(), any(), anyInt());
        verify(clickHousePreparedStatement).setObject(anyInt(), any(), anyInt(), anyInt());
        verify(clickHousePreparedStatement).setRef(anyInt(), any(Ref.class));
        verify(clickHousePreparedStatement).setRowId(anyInt(), any(RowId.class));
        verify(clickHousePreparedStatement).setSQLXML(anyInt(), any(SQLXML.class));
        verify(clickHousePreparedStatement).setTime(anyInt(), any(Time.class));
        verify(clickHousePreparedStatement).setTimestamp(anyInt(), any(Timestamp.class));
        verify(clickHousePreparedStatement).setTimestamp(anyInt(), any(Timestamp.class), any(Calendar.class));
        verify(clickHousePreparedStatement).setURL(anyInt(), any(URL.class));
        verify(clickHousePreparedStatement).setBinaryStream(anyInt(), any(InputStream.class));
        verify(clickHousePreparedStatement).setBinaryStream(anyInt(), any(InputStream.class), anyInt());
        verify(clickHousePreparedStatement).setBinaryStream(anyInt(), any(InputStream.class), anyLong());
        verify(clickHousePreparedStatement).setNClob(42, reader);
        verify(clickHousePreparedStatement).setTime(anyInt(), any(Time.class), any(Calendar.class));
        verify(clickHousePreparedStatement).setTimestamp(anyInt(), any(Timestamp.class), any(Calendar.class));
        verify(clickHousePreparedStatement).setArray(anyInt(), any(Array.class));
        verify(clickHousePreparedStatement).setBlob(anyInt(), any(Blob.class));
        verify(clickHousePreparedStatement).setDate(anyInt(), any(Date.class), any(Calendar.class));
    }

    @Test
    public void testPreparedStatementConfig() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("INSERT INTO test VALUES( ? , ?)", 1);
        preparedStatement.setInt(1, 1);
        preparedStatement.setString(2, "a");
        preparedStatement.getUpdateCount();
        preparedStatement.setFetchDirection(1);
        preparedStatement.getFetchDirection();
        preparedStatement.getResultSetConcurrency();
        preparedStatement.getResultSetType();
        preparedStatement.isClosed();
        preparedStatement.setPoolable(false);
        preparedStatement.isPoolable();
        preparedStatement.getWarnings();
        preparedStatement.clearWarnings();
        preparedStatement.setCursorName("test");
        preparedStatement.setMaxFieldSize(11);
        preparedStatement.getMaxFieldSize();
        preparedStatement.setMaxRows(10);
        preparedStatement.getMaxRows();
        preparedStatement.getParameterMetaData();
        preparedStatement.setEscapeProcessing(true);
        preparedStatement.setFetchSize(1);
        preparedStatement.getFetchSize();
        preparedStatement.setQueryTimeout(1);
        preparedStatement.getQueryTimeout();
        Connection connection = preparedStatement.getConnection();

        preparedStatement.execute();

        preparedStatement.getMoreResults();
        preparedStatement.getMoreResults(1);
        preparedStatement.getResultSetHoldability();
        preparedStatement.getMetaData();
        preparedStatement.getResultSet();

        preparedStatement.close();
        verify(clickHousePreparedStatement).getUpdateCount();
        verify(clickHousePreparedStatement).getMoreResults();
        verify(clickHousePreparedStatement).setFetchDirection(anyInt());
        verify(clickHousePreparedStatement).getFetchDirection();
        verify(clickHousePreparedStatement).getResultSetType();
        verify(clickHousePreparedStatement).isClosed();
        verify(clickHousePreparedStatement).setPoolable(anyBoolean());
        verify(clickHousePreparedStatement).getWarnings();
        verify(clickHousePreparedStatement).clearWarnings();
        verify(clickHousePreparedStatement).setCursorName(anyString());
        verify(clickHousePreparedStatement).setMaxFieldSize(anyInt());
        verify(clickHousePreparedStatement).getMaxFieldSize();
        verify(clickHousePreparedStatement).setMaxRows(anyInt());
        verify(clickHousePreparedStatement).getMaxRows();
        verify(clickHousePreparedStatement).setEscapeProcessing(anyBoolean());
        verify(clickHousePreparedStatement).getResultSetConcurrency();
        verify(clickHousePreparedStatement).getResultSetConcurrency();
        verify(clickHousePreparedStatement).getResultSetType();
        verify(clickHousePreparedStatement).getMetaData();
        verify(clickHousePreparedStatement).getParameterMetaData();
        verify(clickHousePreparedStatement).getMoreResults(anyInt());
        verify(clickHousePreparedStatement).setFetchSize(anyInt());
        verify(clickHousePreparedStatement).getFetchSize();
        verify(clickHousePreparedStatement).getQueryTimeout();
        verify(clickHousePreparedStatement).setQueryTimeout(anyInt());
        verify(clickHousePreparedStatement).getResultSet();
        assertThat(connection, CoreMatchers.<Connection>is(swClickhouseConnection));
    }

    @Test
    public void testExecuteQuery() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("SELECT * FROM test", 1, 1, 1);
        ResultSet resultSet = preparedStatement.executeQuery();

        preparedStatement.close();

        verify(clickHousePreparedStatement).executeQuery();
        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeQuery", "SELECT * FROM test");
    }

    @Test
    public void testExecute() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("SELECT * FROM test", 1, 1, 1);
        preparedStatement.execute();

        preparedStatement.close();

        verify(clickHousePreparedStatement).execute();
        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/execute", "SELECT * FROM test");
    }

    @Test
    public void testQuerySqlWithSql() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("SELECT * FROM test", 1);
        ResultSet resultSet = preparedStatement.executeQuery("SELECT * FROM test");

        preparedStatement.getGeneratedKeys();
        preparedStatement.close();

        verify(clickHousePreparedStatement).executeQuery(anyString());
        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeQuery", "SELECT * FROM test");

    }

    @Test
    public void testInsertWithAutoGeneratedKey() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("INSERT INTO test VALUES(?)", 1);
        boolean insertCount = preparedStatement.execute("INSERT INTO test VALUES(1)", 1);
        preparedStatement.close();

        verify(clickHousePreparedStatement).execute(anyString(), anyInt());
        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/execute", "INSERT INTO test VALUES(1)");

    }

    @Test
    public void testInsertWithIntColumnIndexes() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("INSERT INTO test VALUES(?)", 1);
        boolean insertCount = preparedStatement.execute("INSERT INTO test VALUES(1)", new int[]{1, 2});
        preparedStatement.close();

        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/execute", "INSERT INTO test VALUES(1)");

    }

    @Test
    public void testInsertWithStringColumnIndexes() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("INSERT INTO test VALUES(?)", 1);
        boolean insertCount = preparedStatement.execute("INSERT INTO test VALUES(1)", new String[]{"1", "2"});
        preparedStatement.close();

        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/execute", "INSERT INTO test VALUES(1)");

    }

    @Test
    public void testExecuteWithSQL() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("UPDATE test SET  a = ?");
        preparedStatement.setString(1, "a");
        boolean updateCount = preparedStatement.execute("UPDATE test SET  a = 1");
        preparedStatement.cancel();
        preparedStatement.close();

        verify(clickHousePreparedStatement).execute(anyString());
        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/execute", "UPDATE test SET  a = 1");

    }

    @Test
    public void testExecuteUpdate() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("UPDATE test SET  a = ?");
        preparedStatement.setString(1, "a");
        int updateCount = preparedStatement.executeUpdate();
        preparedStatement.cancel();
        preparedStatement.close();

        verify(clickHousePreparedStatement).executeUpdate();
        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeUpdate", "UPDATE test SET  a = ?");

    }

    @Test
    public void testUpdateSql() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("UPDATE test SET  a = ?");

        int updateCount = preparedStatement.executeUpdate("UPDATE test SET  a = 1");
        preparedStatement.cancel();
        preparedStatement.close();

        verify(clickHousePreparedStatement).executeUpdate(anyString());
        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeUpdate", "UPDATE test SET  a = 1");

    }

    @Test
    public void testUpdateWithAutoGeneratedKey() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("UPDATE test SET  a = ?");

        int updateCount = preparedStatement.executeUpdate("UPDATE test SET  a = 1", 1);
        preparedStatement.cancel();
        preparedStatement.close();

        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeUpdate", "UPDATE test SET  a = 1");
    }

    @Test
    public void testUpdateWithIntColumnIndexes() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("UPDATE test SET  a = ?");

        int updateCount = preparedStatement.executeUpdate("UPDATE test SET  a = 1", new int[]{1});
        preparedStatement.cancel();
        preparedStatement.close();

        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeUpdate", "UPDATE test SET  a = 1");

    }

    @Test
    public void testUpdateWithStringColumnIndexes() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("UPDATE test SET  a = ?");

        int updateCount = preparedStatement.executeUpdate("UPDATE test SET  a = 1", new String[]{"1"});
        preparedStatement.cancel();
        preparedStatement.close();

        verify(clickHousePreparedStatement).close();
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeUpdate", "UPDATE test SET  a = 1");
    }

    @Test
    public void testBatch() throws SQLException, MalformedURLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("UPDATE test SET a = ? WHERE b = ?");
        preparedStatement.setShort(1, (short) 12);
        preparedStatement.setTime(2, new Time(System.currentTimeMillis()));
        preparedStatement.addBatch();
        int[] resultSet = preparedStatement.executeBatch();
        preparedStatement.clearBatch();

        verify(clickHousePreparedStatement).executeBatch();
        verify(clickHousePreparedStatement).addBatch();
        verify(clickHousePreparedStatement).clearBatch();

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeBatch", "");

    }

    @Test
    public void testQueryWithMultiHost() throws SQLException {
        PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("SELECT * FROM test WHERE a = ? or b = ? or c=? or d = ?", 1, 1);
        preparedStatement.setAsciiStream(1, inputStream);
        preparedStatement.setAsciiStream(2, inputStream, 10);
        preparedStatement.setAsciiStream(3, inputStream, 1000000L);
        preparedStatement.setCharacterStream(4, reader);
        ResultSet resultSet = preparedStatement.executeQuery();

        preparedStatement.close();

        verify(clickHousePreparedStatement).executeQuery();
        verify(clickHousePreparedStatement).close();
    }

    @Test(expected = SQLException.class)
    public void testMultiHostWithException() throws SQLException {
        when(clickHousePreparedStatement.executeQuery()).thenThrow(new SQLException());
        try {
            PreparedStatement preparedStatement = swClickhouseConnection.prepareStatement("SELECT * FROM test WHERE a = ? or b = ? or c=? or d = ? or e=?");
            preparedStatement.setBigDecimal(1, new BigDecimal(10000));
            preparedStatement.setBlob(2, inputStream);
            preparedStatement.setBlob(3, inputStream, 1000000L);
            preparedStatement.setByte(3, (byte) 1);
            preparedStatement.setBytes(4, new byte[]{1, 2});
            preparedStatement.setLong(5, 100L);

            ResultSet resultSet = preparedStatement.executeQuery();

            preparedStatement.close();
        } finally {
            verify(clickHousePreparedStatement).executeQuery();
            verify(clickHousePreparedStatement, times(0)).close();
            verify(clickHousePreparedStatement).setBigDecimal(anyInt(), any(BigDecimal.class));
            verify(clickHousePreparedStatement).setBlob(anyInt(), any(InputStream.class));
            verify(clickHousePreparedStatement).setBlob(anyInt(), any(InputStream.class), anyLong());
            verify(clickHousePreparedStatement).setByte(anyInt(), anyByte());

            TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
            List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
            assertThat(spans.size(), is(1));
            assertDBSpan(spans.get(0), "ClickHouse/JDBC/PreparedStatement/executeQuery", "SELECT * FROM test WHERE a = ? or b = ? or c=? or d = ? or e=?");

            List<LogDataEntity> logData = SpanHelper.getLogs(spans.get(0));
            Assert.assertThat(logData.size(), is(1));
            assertThat(logData.size(), is(1));
            assertDBSpanLog(logData.get(0));
        }

    }

}
