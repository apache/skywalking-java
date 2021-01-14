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

package org.apache.skywalking.oap.server.storage.plugin.jdbc.mysql;

import org.apache.skywalking.oap.server.library.client.jdbc.hikaricp.JDBCHikariCPClient;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.storage.plugin.jdbc.h2.dao.H2LogQueryDAO;

public class MySQLLogQueryDAO extends H2LogQueryDAO {

    public MySQLLogQueryDAO(final JDBCHikariCPClient h2Client,
                            final ModuleManager manager,
                            final int maxSizeOfArrayColumn, final int numOfSearchValuesPerTag) {
        super(h2Client, manager, maxSizeOfArrayColumn, numOfSearchValuesPerTag);
    }

    @Override
    protected String buildCountStatement(String sql) {
        return "select count(1) total " + sql;
    }

    protected void buildLimit(StringBuilder sql, int from, int limit) {
        sql.append(" LIMIT ").append(from).append(", ").append(limit);
    }
}
