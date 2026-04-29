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

import java.util.List;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.jdbc.JDBCPluginConfig;
import org.apache.skywalking.apm.plugin.jdbc.clickhouse.v32.ClickHousePrepareStatementTracing;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verify that {@link ClickHousePrepareStatementTracing} truncates the SQL body
 * recorded on the exit span according to {@link JDBCPluginConfig.Plugin.JDBC#SQL_BODY_MAX_LENGTH}.
 */
@RunWith(TracingSegmentRunner.class)
public class ClickHousePrepareStatementTracingTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private ConnectionInfo connectionInfo;

    private int originalLimit;

    @Before
    public void setUp() {
        connectionInfo = new ConnectionInfo(
                ComponentsDefine.CLICKHOUSE_JDBC_DRIVER, "ClickHouse", "127.0.0.1", 8123, "test");
        originalLimit = JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH;
    }

    @After
    public void tearDown() {
        JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH = originalLimit;
    }

    @Test
    public void shortSqlIsRecordedAsIs() throws Exception {
        JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH = 2048;
        String sql = "SELECT 1";

        ClickHousePrepareStatementTracing.of(connectionInfo, "execute", sql, () -> Boolean.TRUE);

        assertThat(dbStatementTagValue(), is(sql));
    }

    @Test
    public void longSqlIsTruncatedToConfiguredLimit() throws Exception {
        JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH = 16;
        // Longer than the configured limit, so the helper must truncate to the first 16 chars and append "..."
        String sql = "SELECT * FROM table_with_many_cols";

        ClickHousePrepareStatementTracing.of(connectionInfo, "execute", sql, () -> Boolean.TRUE);

        assertThat(dbStatementTagValue(), is(sql.substring(0, 16) + "..."));
    }

    @Test
    public void negativeLimitDisablesTruncation() throws Exception {
        JDBCPluginConfig.Plugin.JDBC.SQL_BODY_MAX_LENGTH = -1;
        StringBuilder builder = new StringBuilder("SELECT ");
        for (int i = 0; i < 1000; i++) {
            builder.append("a, ");
        }
        builder.append("a FROM t");
        String sql = builder.toString();

        ClickHousePrepareStatementTracing.of(connectionInfo, "execute", sql, () -> Boolean.TRUE);

        assertThat(dbStatementTagValue(), is(sql));
    }

    private String dbStatementTagValue() {
        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(1));
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegments.get(0));
        assertThat(spans.size(), is(1));
        List<TagValuePair> tags = SpanHelper.getTags(spans.get(0));
        // tag order: db.type, db.instance, db.statement
        return (String) tags.get(2).getValue();
    }
}
