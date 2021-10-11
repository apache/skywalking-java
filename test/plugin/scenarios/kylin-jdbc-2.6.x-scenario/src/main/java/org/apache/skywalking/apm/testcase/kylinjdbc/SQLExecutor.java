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

package org.apache.skywalking.apm.testcase.kylinjdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SQLExecutor implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(SQLExecutor.class);
    private Connection connection;

    public SQLExecutor() throws SQLException {
        try {
            Class.forName("org.apache.kylin.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            LOGGER.error(ex);
        }
        connection = DriverManager.getConnection(KylinJdbcConfig.getUrl(), KylinJdbcConfig.getUserName(),
                KylinJdbcConfig.getPassword());
    }

    public void execute(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(sql);
    }

    public void queryData(String sql, Integer id) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.executeQuery();
    }

    public void closeConnection() throws SQLException {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    @Override
    public void close() throws Exception {
        closeConnection();
    }
}
