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

import org.junit.Test;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLParserTest {
    @Test
    public void testParseMysqlJDBCURLWithHost() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:mysql//primaryhost/test");
        assertThat(connectionInfo.getDBType(), is("Mysql"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("primaryhost:3306"));
    }

    @Test
    public void testParseMysqlJDBCURLWithoutDB() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:mysql//primaryhost?profileSQL=true");
        assertThat(connectionInfo.getDBType(), is("Mysql"));
        assertThat(connectionInfo.getDatabaseName(), is(""));
        assertThat(connectionInfo.getDatabasePeer(), is("primaryhost:3306"));
    }

    @Test
    public void testParseMysqlJDBCURLWithHostAndPort() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:mysql//primaryhost:3307/test?profileSQL=true");
        assertThat(connectionInfo.getDBType(), is("Mysql"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("primaryhost:3307"));
    }

    @Test
    public void testParseMysqlJDBCURLWithMultiHost() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:mysql//primaryhost:3307,secondaryhost1,secondaryhost2/test?profileSQL=true");
        assertThat(connectionInfo.getDBType(), is("Mysql"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("primaryhost:3307,secondaryhost1:3306,secondaryhost2:3306"));
    }

    @Test
    public void testParseMysqlJDBCURLWitOutDatabase() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:mysql//primaryhost:3307?profileSQL=true");
        assertThat(connectionInfo.getDBType(), is("Mysql"));
        assertThat(connectionInfo.getDatabaseName(), is(""));
        assertThat(connectionInfo.getDatabasePeer(), is("primaryhost:3307"));
    }

    @Test
    public void testParseMysqlJDBCURLWithConnectorJs() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:mysql:replication://master,slave1,slave2,slave3/test");
        assertThat(connectionInfo.getDBType(), is("Mysql"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("master:3306,slave1:3306,slave2:3306,slave3:3306"));
    }

    @Test
    public void testParseOracleJDBCURLWithHost() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:@localhost:orcl");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1521"));
    }

    @Test
    public void testParseOracleJDBCURLWithHostAndPort() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:@localhost:1522:orcl");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1522"));
    }

    @Test
    public void testParseOracleSID() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:@localhost:1522/orcl");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1522"));
    }

    @Test
    public void testParseOracleServiceName() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:@//localhost:1531/orcl");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1531"));
    }

    @Test
    public void testParseOracleTNSName() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST= localhost )(PORT= 1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=orcl)))");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1521"));
    }

    @Test
    public void testParseOracleLowerTNSName() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:@(description=(address=(protocol=tcp)(host= localhost )(port= 1521))(connect_data=(server=dedicated)(service_name=orcl)))");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1521"));
    }

    @Test
    public void testParseOracleTNSNameWithMultiAddress() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL= TCP)(HOST=hostA)(PORT= 1523 ))(ADDRESS=(PROTOCOL=TCP)(HOST=hostB)(PORT= 1521 )))(SOURCE_ROUTE=yes)(CONNECT_DATA=(SERVICE_NAME=orcl)))");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("hostA:1523,hostB:1521"));
    }

    @Test
    public void testParseOracleJDBCURLWithUserNameAndPassword() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:oracle:thin:scott/tiger@myhost:1521:orcl");
        assertThat(connectionInfo.getDBType(), is("Oracle"));
        assertThat(connectionInfo.getDatabaseName(), is("orcl"));
        assertThat(connectionInfo.getDatabasePeer(), is("myhost:1521"));
    }

    @Test
    public void testParseH2JDBCURLWithEmbedded() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:h2:file:/data/sample");
        assertThat(connectionInfo.getDBType(), is("H2"));
        assertThat(connectionInfo.getDatabaseName(), is("/data/sample"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseH2JDBCURLWithEmbeddedRunningInWindows() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:h2:file:C:/data/sample");
        assertThat(connectionInfo.getDBType(), is("H2"));
        assertThat(connectionInfo.getDatabaseName(), is("C:/data/sample"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseH2JDBCURLWithMemoryMode() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:h2:mem:test_mem");
        assertThat(connectionInfo.getDBType(), is("H2"));
        assertThat(connectionInfo.getDatabaseName(), is("test_mem"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseH2JDBCURL() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:h2:tcp://localhost:8084/~/sample");
        assertThat(connectionInfo.getDBType(), is("H2"));
        assertThat(connectionInfo.getDatabaseName(), is("sample"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:8084"));
    }

    @Test
    public void testParseMariadbJDBCURLWithHost() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:mariadb//primaryhost/test");
        assertThat(connectionInfo.getDBType(), is("Mariadb"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("primaryhost:3306"));
    }

    @Test
    public void testParseClickhouseJDBCURL() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:clickhouse://localhost:8123/test");
        assertThat(connectionInfo.getDBType(), is("ClickHouse"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:8123"));
    }

    @Test
    public void testParseImpalaJDBCURL() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:impala://localhost:21050/test;AuthMech=3;UID=UserName;PWD=Password");
        assertThat(connectionInfo.getDBType(), is("Impala"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:21050"));
    }

    @Test
    public void testParseImpalaJDBCURLWithSchema() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:impala://localhost:21050/test");
        assertThat(connectionInfo.getDBType(), is("Impala"));
        assertThat(connectionInfo.getDatabaseName(), is("test"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:21050"));
    }

    @Test
    public void testParseImpalaJDBCURLWithoutSchema() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:impala://localhost:21050");
        assertThat(connectionInfo.getDBType(), is("Impala"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:21050"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithHostAndParams() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql://localhost:5432/testdb?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&characterSetResults=utf8&useSSL=false&allowMultiQueries=true");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is("testdb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:5432"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithMultiHost() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql://localhost1:5432,localhost2:5433/testdb?target_session_attrs=any&application_name=myapp");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is("testdb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost1:5432,localhost2:5433"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithHostNoPort() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql://localhost/testdb");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is("testdb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:5432"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithHostNoDb() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql://localhost:5432");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is(""));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:5432"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithHostNoPortAndDb() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql://localhost");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is(""));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:5432"));
    }

    @Test
    public void testParsePostgresqlJDBCURLEmpty() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql://");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is(""));
        assertThat(connectionInfo.getDatabasePeer(), is(":5432"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithNamedParams() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql:///testdb?host=localhost&port=5433");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is("testdb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:5433"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithSingleIpv6() {
        ConnectionInfo connectionInfo = new URLParser().parser("jdbc:postgresql://[2001:db8::1234]/testdb");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is("testdb"));
        assertThat(connectionInfo.getDatabasePeer(), is("[2001:db8::1234]:5432"));
    }

    @Test
    public void testParsePostgresqlJDBCURLWithMultiIpv6() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:postgresql://[2001:db8::1234],[2001:db8::1235]/testdb");
        assertThat(connectionInfo.getDBType(), is("PostgreSQL"));
        assertThat(connectionInfo.getDatabaseName(), is("testdb"));
        assertThat(connectionInfo.getDatabasePeer(), is("[2001:db8::1234]:5432,[2001:db8::1235]:5432"));
    }

    @Test
    public void testParseOceanBaseJDBCURL() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:oceanbase://localhost:2881/mydb?user=root@sys&password=pass&pool=false&useBulkStmts=true&rewriteBatchedStatements=false&useServerPrepStmts=true");
        assertThat(connectionInfo.getDBType(), is("OceanBase"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:2881"));
    }

    @Test
    public void testParseOceanBaseJDBCURLWithMultiHosts() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:oceanbase://primaryhost:2888,secondaryhost1,secondaryhost2/mydb?user=root@sys&password=pass&pool=false&useBulkStmts=true&rewriteBatchedStatements=false&useServerPrepStmts=true");
        assertThat(connectionInfo.getDBType(), is("OceanBase"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("primaryhost:2888,secondaryhost1:2881,secondaryhost2:2881"));
    }

    @Test
    public void testParseDerbyJDBCURLWithDirMode() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:derby:directory:mydb");
        assertThat(connectionInfo.getDBType(), is("Derby"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseDerbyJDBCURLWithMemMode() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:derby:memory:mydb;create=true");
        assertThat(connectionInfo.getDBType(), is("Derby"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseDerbyJDBCURLWithClassPathMode() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:derby:classpath:/test/mydb");
        assertThat(connectionInfo.getDBType(), is("Derby"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseDerbyJDBCURLWithJarMode() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:derby:jar:(C:/dbs.jar)test/mydb");
        assertThat(connectionInfo.getDBType(), is("Derby"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseDerbyJDBCURLWithEmbeddedMode() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:derby:test/mydb;create=true");
        assertThat(connectionInfo.getDBType(), is("Derby"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseDerbyJDBCURLWithMemModeAndClientServerMode() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:derby://localhost:1527/memory:/test/mydb;create=true");
        assertThat(connectionInfo.getDBType(), is("Derby"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1527"));
    }

    @Test
    public void testParseDerbyJDBCURLWithClientServerMode() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:derby://localhost:1527/mydb;create=true;user=root;password=pass");
        assertThat(connectionInfo.getDBType(), is("Derby"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:1527"));
    }

    @Test
    public void testParseDB2JDBCURL() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:db2://localhost:50000/mydb:user=root;password=pass");
        assertThat(connectionInfo.getDBType(), is("DB2"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:50000"));
    }

    @Test
    public void testParseDB2JDBCURLWithoutHost() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:db2:mydb:user=root;password=pass");
        assertThat(connectionInfo.getDBType(), is("DB2"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:50000"));
    }

    @Test
    public void testParseSqliteJDBCURL() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:sqlite:C/test/mydb.db");
        assertThat(connectionInfo.getDBType(), is("Sqlite"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb.db"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseSqliteJDBCURLWithMem() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:sqlite::memory:?jdbc.explicit_readonly=true");
        assertThat(connectionInfo.getDBType(), is("Sqlite"));
        assertThat(connectionInfo.getDatabaseName(), is(""));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseSqliteJDBCURLWithResource() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:sqlite::resource:org/test/mydb.db");
        assertThat(connectionInfo.getDBType(), is("Sqlite"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb.db"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:-1"));
    }

    @Test
    public void testParseSybaseJDBCURL() {
        ConnectionInfo connectionInfo = new URLParser().parser(
            "jdbc:sybase:Tds:localhost:5000/mydb?charset=utf-8");
        assertThat(connectionInfo.getDBType(), is("Sybase"));
        assertThat(connectionInfo.getDatabaseName(), is("mydb"));
        assertThat(connectionInfo.getDatabasePeer(), is("localhost:5000"));
    }
}