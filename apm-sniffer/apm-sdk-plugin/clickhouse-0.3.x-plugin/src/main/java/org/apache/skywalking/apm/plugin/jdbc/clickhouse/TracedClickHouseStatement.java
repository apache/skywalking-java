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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;
import java.util.Map;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import ru.yandex.clickhouse.ClickHouseExternalData;
import ru.yandex.clickhouse.ClickHouseStatement;
import ru.yandex.clickhouse.Writer;
import ru.yandex.clickhouse.response.ClickHouseResponse;
import ru.yandex.clickhouse.response.ClickHouseResponseSummary;
import ru.yandex.clickhouse.settings.ClickHouseQueryParam;
import ru.yandex.clickhouse.util.ClickHouseRowBinaryInputStream;
import ru.yandex.clickhouse.util.ClickHouseStreamCallback;

/**
 * The {@link ru.yandex.clickhouse.ClickHouseStatementImpl} instance wrapper.
 */
public class TracedClickHouseStatement implements ClickHouseStatement {

    private final ClickHouseStatement delegate;
    private final ConnectionInfo connectionInfo;

    public TracedClickHouseStatement(ClickHouseStatement delegate, ConnectionInfo connectionInfo) {
        this.delegate = delegate;
        this.connectionInfo = connectionInfo;
    }

    @Override
    public ClickHouseResponse executeQueryClickhouseResponse(String sql) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQueryClickhouseResponse", sql,
                () -> delegate.executeQueryClickhouseResponse(sql));
    }

    @Override
    public ClickHouseResponse executeQueryClickhouseResponse(String sql,
            Map<ClickHouseQueryParam, String> additionalDBParams) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQueryClickhouseResponse", sql,
                () -> delegate.executeQueryClickhouseResponse(sql, additionalDBParams));
    }

    @Override
    public ClickHouseResponse executeQueryClickhouseResponse(String sql,
            Map<ClickHouseQueryParam, String> additionalDBParams, Map<String, String> additionalRequestParams)
            throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQueryClickhouseResponse", sql,
                () -> delegate.executeQueryClickhouseResponse(sql, additionalDBParams, additionalRequestParams));
    }

    @Override
    public ClickHouseRowBinaryInputStream executeQueryClickhouseRowBinaryStream(String sql) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQueryClickhouseRowBinaryStream", sql,
                () -> delegate.executeQueryClickhouseRowBinaryStream(sql));
    }

    @Override
    public ClickHouseRowBinaryInputStream executeQueryClickhouseRowBinaryStream(String sql,
            Map<ClickHouseQueryParam, String> additionalDBParams) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQueryClickhouseRowBinaryStream", sql,
                () -> delegate.executeQueryClickhouseRowBinaryStream(sql, additionalDBParams));
    }

    @Override
    public ClickHouseRowBinaryInputStream executeQueryClickhouseRowBinaryStream(String sql,
            Map<ClickHouseQueryParam, String> additionalDBParams, Map<String, String> additionalRequestParams)
            throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQueryClickhouseRowBinaryStream", sql,
                () -> delegate.executeQueryClickhouseRowBinaryStream(sql, additionalDBParams, additionalRequestParams));
    }

    @Override
    public ResultSet executeQuery(String sql, Map<ClickHouseQueryParam, String> additionalDBParams)
            throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQuery", sql,
                () -> delegate.executeQuery(sql, additionalDBParams));
    }

    @Override
    public ResultSet executeQuery(String sql, Map<ClickHouseQueryParam, String> additionalDBParams,
            List<ClickHouseExternalData> externalData) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQuery", sql,
                () -> delegate.executeQuery(sql, additionalDBParams, externalData));
    }

    @Override
    public ResultSet executeQuery(String sql, Map<ClickHouseQueryParam, String> additionalDBParams,
            List<ClickHouseExternalData> externalData, Map<String, String> additionalRequestParams)
            throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQuery", sql,
                () -> delegate.executeQuery(sql, additionalDBParams, externalData, additionalRequestParams));
    }

    @Override
    public void sendStream(InputStream content, String table, Map<ClickHouseQueryParam, String> additionalDBParams)
            throws SQLException {
        delegate.sendStream(content, table, additionalDBParams);
    }

    @Override
    public void sendStream(InputStream content, String table) throws SQLException {
        delegate.sendStream(content, table);
    }

    @Override
    public void sendRowBinaryStream(String sql, Map<ClickHouseQueryParam, String> additionalDBParams,
            ClickHouseStreamCallback callback) throws SQLException {
        sendRowBinaryStream(sql, additionalDBParams, callback);
    }

    @Override
    public void sendRowBinaryStream(String sql, ClickHouseStreamCallback callback) throws SQLException {
        sendRowBinaryStream(sql, callback);
    }

    @Override
    public void sendNativeStream(String sql, Map<ClickHouseQueryParam, String> additionalDBParams,
            ClickHouseStreamCallback callback) throws SQLException {
        sendNativeStream(sql, additionalDBParams, callback);
    }

    @Override
    public void sendNativeStream(String sql, ClickHouseStreamCallback callback) throws SQLException {
        sendNativeStream(sql, callback);
    }

    @Override
    public void sendCSVStream(InputStream content, String table, Map<ClickHouseQueryParam, String> additionalDBParams)
            throws SQLException {
        sendCSVStream(content, table, additionalDBParams);
    }

    @Override
    public void sendCSVStream(InputStream content, String table) throws SQLException {
        sendCSVStream(content, table);
    }

    @Override
    public void sendStreamSQL(InputStream content, String sql, Map<ClickHouseQueryParam, String> additionalDBParams)
            throws SQLException {
        sendStreamSQL(content, sql, additionalDBParams);
    }

    @Override
    public void sendStreamSQL(InputStream content, String sql) throws SQLException {
        sendStreamSQL(content, sql);
    }

    @Override
    public Writer write() {
        return delegate.write();
    }

    @Override
    public ClickHouseResponseSummary getResponseSummary() {
        return delegate.getResponseSummary();
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeQuery", sql,
                () -> delegate.executeQuery(sql));
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeUpdate", sql,
                () -> delegate.executeUpdate(sql));
    }

    @Override
    public void close() throws SQLException {
        delegate.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return delegate.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        delegate.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return delegate.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        delegate.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        delegate.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return delegate.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        delegate.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        delegate.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        delegate.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        delegate.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "execute", sql, () -> delegate.execute(sql));
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return delegate.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return delegate.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return delegate.getMoreResults();
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return delegate.getFetchDirection();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        delegate.setFetchDirection(direction);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return delegate.getFetchSize();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        delegate.setFetchSize(rows);
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return delegate.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return delegate.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        delegate.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        delegate.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeBatch", "", delegate::executeBatch);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return delegate.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return delegate.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeUpdate", sql,
                () -> delegate.executeUpdate(sql, autoGeneratedKeys));
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeUpdate", sql,
                () -> delegate.executeUpdate(sql, columnIndexes));
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "executeUpdate", sql,
                () -> delegate.executeUpdate(sql, columnNames));
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "execute", sql,
                () -> delegate.execute(sql, autoGeneratedKeys));
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "execute", sql,
                () -> delegate.execute(sql, columnIndexes));
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return ClickHouseStatementTracingWrapper.of(connectionInfo, "execute", sql,
                () -> delegate.execute(sql, columnNames));
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return delegate.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return delegate.isPoolable();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        delegate.setPoolable(poolable);
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        delegate.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return delegate.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
