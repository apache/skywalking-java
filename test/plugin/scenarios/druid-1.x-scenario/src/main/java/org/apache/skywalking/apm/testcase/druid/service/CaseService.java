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

package org.apache.skywalking.apm.testcase.druid.service;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.skywalking.apm.testcase.druid.MySQLConfig;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class CaseService {

    public static DruidDataSource DS;
    private static final String CREATE_TABLE_SQL = "CREATE TABLE test_DRUID(id VARCHAR(1) PRIMARY KEY, value VARCHAR(1) NOT NULL)";
    private static final String INSERT_DATA_SQL = "INSERT INTO test_DRUID(id, value) VALUES(1,1)";
    private static final String QUERY_DATA_SQL = "SELECT id, value FROM test_DRUID WHERE id=1";
    private static final String DELETE_DATA_SQL = "DELETE FROM test_DRUID WHERE id=1";
    private static final String DROP_TABLE_SQL = "DROP table test_DRUID";

    static {
        DS = new DruidDataSource();
        try {
            DS.setUrl(MySQLConfig.getUrl());
            DS.setUsername(MySQLConfig.getUserName());
            DS.setPassword(MySQLConfig.getPassword());
            DS.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCase() {
        sqlExecutor(CREATE_TABLE_SQL);
        sqlExecutor(INSERT_DATA_SQL);
        sqlExecutor(QUERY_DATA_SQL);
        sqlExecutor(DELETE_DATA_SQL);
        sqlExecutor(DROP_TABLE_SQL);
    }

    public void sqlExecutor(String sql) {
        try (Connection conn = DS.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
