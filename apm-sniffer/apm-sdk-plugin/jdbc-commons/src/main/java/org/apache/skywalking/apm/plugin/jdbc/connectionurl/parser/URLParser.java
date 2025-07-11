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

package org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser;

import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

/**
 * {@link URLParser#parser(String)} support parse the connection url, such as Mysql, Oracle, H2 Database. But there are
 * some url cannot be parsed, such as Oracle connection url with multiple host.
 */
public class URLParser {

    private static final String MYSQL_JDBC_URL_PREFIX = "jdbc:mysql";
    private static final String ORACLE_JDBC_URL_PREFIX = "jdbc:oracle";
    private static final String H2_JDBC_URL_PREFIX = "jdbc:h2";
    private static final String POSTGRESQL_JDBC_URL_PREFIX = "jdbc:postgresql";
    private static final String MARIADB_JDBC_URL_PREFIX = "jdbc:mariadb";
    private static final String MSSQL_JTDS_URL_PREFIX = "jdbc:jtds:sqlserver:";
    private static final String MSSQL_JDBC_URL_PREFIX = "jdbc:sqlserver:";
    private static final String KYLIN_JDBC_URL_PREFIX = "jdbc:kylin";
    private static final String IMPALA_JDBC_URL_PREFIX = "jdbc:impala";
    private static final String CLICKHOUSE_JDBC_URL_PREFIX = "jdbc:clickhouse";
    private static final String DERBY_JDBC_URL_PREFIX = "jdbc:derby:";
    private static final String SQLITE_JDBC_URL_PREFIX = "jdbc:sqlite:";
    private static final String DB2_JDBC_URL_PREFIIX = "jdbc:db2:";
    private static final String SYBASE_JDBC_URL_PREFIX = "jdbc:sybase:tds:";
    private static final String OCEANBASE_JDBC_URL_PREFIX = "jdbc:oceanbase:";
    private static final String DM_JDBC_URL_PREFIX = "jdbc:dm:";

    public static ConnectionInfo parser(String url) {
        ConnectionURLParser parser = null;
        String lowerCaseUrl = url.toLowerCase();
        if (lowerCaseUrl.startsWith(MYSQL_JDBC_URL_PREFIX)) {
            parser = new MysqlURLParser(url);
        } else if (lowerCaseUrl.startsWith(ORACLE_JDBC_URL_PREFIX)) {
            parser = new OracleURLParser(url);
        } else if (lowerCaseUrl.startsWith(H2_JDBC_URL_PREFIX)) {
            parser = new H2URLParser(url);
        } else if (lowerCaseUrl.startsWith(POSTGRESQL_JDBC_URL_PREFIX)) {
            parser = new PostgreSQLURLParser(url);
        } else if (lowerCaseUrl.startsWith(MARIADB_JDBC_URL_PREFIX)) {
            parser = new MariadbURLParser(url);
        } else if (lowerCaseUrl.startsWith(MSSQL_JTDS_URL_PREFIX)) {
            parser = new MssqlJtdsURLParser(url);
        } else if (lowerCaseUrl.startsWith(MSSQL_JDBC_URL_PREFIX)) {
            parser = new MssqlJdbcURLParser(url);
        } else if (lowerCaseUrl.startsWith(KYLIN_JDBC_URL_PREFIX)) {
            parser = new KylinJdbcURLParser(url);
        } else if (lowerCaseUrl.startsWith(IMPALA_JDBC_URL_PREFIX)) {
            parser = new ImpalaJdbcURLParser(url);
        } else if (lowerCaseUrl.startsWith(CLICKHOUSE_JDBC_URL_PREFIX)) {
            parser = new ClickHouseURLParser(url);
        } else if (lowerCaseUrl.startsWith(DERBY_JDBC_URL_PREFIX)) {
            parser = new DerbyURLParser(url);
        } else if (lowerCaseUrl.startsWith(SQLITE_JDBC_URL_PREFIX)) {
            parser = new SqliteURLParser(url);
        } else if (lowerCaseUrl.startsWith(DB2_JDBC_URL_PREFIIX)) {
            parser = new Db2URLParser(url);
        } else if (lowerCaseUrl.startsWith(SYBASE_JDBC_URL_PREFIX)) {
            parser = new SybaseURLParser(url);
        } else if (lowerCaseUrl.startsWith(OCEANBASE_JDBC_URL_PREFIX)) {
            parser = new OceanBaseURLParser(url);
        } else if (lowerCaseUrl.startsWith(DM_JDBC_URL_PREFIX)) {
            parser = new DMURLParser(url);
        }

        return parser.parse();
    }
}
