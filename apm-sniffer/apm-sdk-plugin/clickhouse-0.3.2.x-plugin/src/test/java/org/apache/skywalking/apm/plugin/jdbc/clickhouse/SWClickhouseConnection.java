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

import java.net.URI;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.util.Calendar;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Executor;

import com.clickhouse.client.ClickHouseConfig;
import com.clickhouse.client.ClickHouseTransaction;
import com.clickhouse.client.ClickHouseVersion;
import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseArray;
import com.clickhouse.jdbc.ClickHouseBlob;
import com.clickhouse.jdbc.ClickHouseClob;
import com.clickhouse.jdbc.ClickHouseXml;
import com.clickhouse.jdbc.ClickHouseStruct;
import com.clickhouse.jdbc.ClickHouseStatement;
import com.clickhouse.jdbc.JdbcConfig;
import com.clickhouse.jdbc.parser.ClickHouseSqlStatement;
import org.apache.skywalking.apm.plugin.jdbc.clickhouse.v32.SWClickHousePreparedStatement;
import org.apache.skywalking.apm.plugin.jdbc.clickhouse.v32.SWClickHouseStatement;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

/**
 * for test
 */
public class SWClickhouseConnection implements ClickHouseConnection {
    private ConnectionInfo connectInfo;
    private final ClickHouseConnection realConnection;

    public SWClickhouseConnection(String url, Properties info, ClickHouseConnection realConnection, ConnectionInfo connectInfo) {
        super();
        this.connectInfo = connectInfo;
        this.realConnection = realConnection;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return realConnection.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return realConnection.isWrapperFor(iface);
    }

    public ClickHouseStatement createStatement() throws SQLException {
        return new SWClickHouseStatement(this, realConnection.createStatement(), this.connectInfo);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new SWClickHousePreparedStatement(this, realConnection.prepareStatement(sql), this.connectInfo, sql);
    }

    @Deprecated
    public CallableStatement prepareCall(String sql) throws SQLException {
        return null;
    }

    public String nativeSQL(String sql) throws SQLException {
        return realConnection.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        realConnection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return realConnection.getAutoCommit();
    }

    public void commit() throws SQLException {
        realConnection.commit();
    }

    public void rollback() throws SQLException {
        realConnection.rollback();
    }

    public void close() throws SQLException {
        realConnection.close();
    }

    public boolean isClosed() throws SQLException {
        return realConnection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return realConnection.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        realConnection.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return realConnection.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        realConnection.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return realConnection.getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        realConnection.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return realConnection.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return realConnection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        realConnection.clearWarnings();
    }

    public ClickHouseStatement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return new SWClickHouseStatement(this, realConnection.createStatement(resultSetType, resultSetConcurrency), this.connectInfo);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency) throws SQLException {
        return new SWClickHousePreparedStatement(this, realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency), this.connectInfo, sql);
    }

    @Override
    public ClickHouseConfig getConfig() {
        return realConnection.getConfig();
    }

    @Override
    public boolean allowCustomSetting() {
        return realConnection.allowCustomSetting();
    }

    @Override
    public String getCurrentDatabase() {
        return realConnection.getCurrentDatabase();
    }

    @Override
    public String getCurrentUser() {
        return realConnection.getCurrentUser();
    }

    @Override
    public Calendar getDefaultCalendar() {
        return realConnection.getDefaultCalendar();
    }

    @Override
    public Optional<TimeZone> getEffectiveTimeZone() {
        return realConnection.getEffectiveTimeZone();
    }

    @Override
    public TimeZone getJvmTimeZone() {
        return realConnection.getJvmTimeZone();
    }

    @Override
    public TimeZone getServerTimeZone() {
        return realConnection.getServerTimeZone();
    }

    @Override
    public ClickHouseVersion getServerVersion() {
        return realConnection.getServerVersion();
    }

    @Override
    public ClickHouseTransaction getTransaction() {
        return realConnection.getTransaction();
    }

    @Override
    public URI getUri() {
        return realConnection.getUri();
    }

    @Override
    public JdbcConfig getJdbcConfig() {
        return realConnection.getJdbcConfig();
    }

    @Override
    public boolean isTransactionSupported() {
        return realConnection.isTransactionSupported();
    }

    @Override
    public boolean isImplicitTransactionSupported() {
        return realConnection.isImplicitTransactionSupported();
    }

    @Override
    public String newQueryId() {
        return realConnection.newQueryId();
    }

    @Override
    public ClickHouseSqlStatement[] parse(String sql, ClickHouseConfig config) {
        return new ClickHouseSqlStatement[0];
    }

    @Deprecated
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return realConnection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        realConnection.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        realConnection.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return realConnection.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return realConnection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return realConnection.setSavepoint(name);
    }

    public void rollback(final Savepoint savepoint) throws SQLException {
        realConnection.rollback(savepoint);
    }

    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        realConnection.releaseSavepoint(savepoint);
    }

    @Deprecated
    public ClickHouseStatement createStatement(int resultSetType, int resultSetConcurrency,
                                               int resultSetHoldability) throws SQLException {
        return new SWClickHouseStatement(this, realConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this.connectInfo);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        return new SWClickHousePreparedStatement(this, realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this.connectInfo, sql);
    }

    @Deprecated
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        return null;
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return new SWClickHousePreparedStatement(this, realConnection.prepareStatement(sql, autoGeneratedKeys), this.connectInfo, sql);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return new SWClickHousePreparedStatement(this, realConnection.prepareStatement(sql, columnIndexes), this.connectInfo, sql);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return new SWClickHousePreparedStatement(this, realConnection.prepareStatement(sql, columnNames), this.connectInfo, sql);
    }

    public ClickHouseClob createClob() throws SQLException {
        return realConnection.createClob();
    }

    public ClickHouseBlob createBlob() throws SQLException {
        return realConnection.createBlob();
    }

    public NClob createNClob() throws SQLException {
        return realConnection.createNClob();
    }

    public ClickHouseXml createSQLXML() throws SQLException {
        return realConnection.createSQLXML();
    }

    public boolean isValid(int timeout) throws SQLException {
        return realConnection.isValid(timeout);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        realConnection.setClientInfo(name, value);
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        realConnection.setClientInfo(properties);
    }

    public String getClientInfo(String name) throws SQLException {
        return realConnection.getClientInfo(name);
    }

    public Properties getClientInfo() throws SQLException {
        return realConnection.getClientInfo();
    }

    public ClickHouseArray createArrayOf(String typeName, Object[] elements) throws SQLException {
        return realConnection.createArrayOf(typeName, elements);
    }

    public ClickHouseStruct createStruct(String typeName, Object[] attributes) throws SQLException {
        return realConnection.createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        realConnection.setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return realConnection.getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        realConnection.abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        realConnection.setNetworkTimeout(executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return realConnection.getNetworkTimeout();
    }

}
