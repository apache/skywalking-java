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

package org.apache.skywalking.apm.testcase.c3p0.mchange.service;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.skywalking.apm.testcase.c3p0.mchange.MysqlConfig;
import org.springframework.stereotype.Service;

@Service
public class CaseService {

    public static ComboPooledDataSource DS;
    private static final String CREATE_TABLE_SQL = "CREATE TABLE test_C3P0(\n" + "id VARCHAR(1) PRIMARY KEY, \n" + "value VARCHAR(1) NOT NULL)";
    private static final String INSERT_DATA_SQL = "INSERT INTO test_C3P0(id, value) VALUES(1,1)";
    private static final String QUERY_DATA_SQL = "SELECT id, value FROM test_C3P0 WHERE id=1";
    private static final String DELETE_DATA_SQL = "DELETE FROM test_C3P0 WHERE id=1";
    private static final String DROP_TABLE_SQL = "DROP table test_C3P0";

    static {
        try {
            DS = new ComboPooledDataSource();
            DS.setDriverClass("com.mysql.jdbc.Driver");
            DS.setJdbcUrl(MysqlConfig.getUrl());
            DS.setUser(MysqlConfig.getUserName());
            DS.setPassword(MysqlConfig.getPassword());
            DS.setAcquireIncrement(1);
            DS.setInitialPoolSize(5);
            DS.setMinPoolSize(1);
            DS.setMaxIdleTime(10);
            DS.setTestConnectionOnCheckin(false);
            DS.setTestConnectionOnCheckout(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCase() {
        sqlExecutor(DS, CREATE_TABLE_SQL);
        sqlExecutor(DS, INSERT_DATA_SQL);
        sqlExecutor(DS, QUERY_DATA_SQL);
        sqlExecutor(DS, DELETE_DATA_SQL);
        sqlExecutor(DS, DROP_TABLE_SQL);
    }

    public void sqlExecutor(ComboPooledDataSource dataSource, String sql) {
        try (Connection conn = dataSource.getConnection();) {
            Statement statement = conn.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
