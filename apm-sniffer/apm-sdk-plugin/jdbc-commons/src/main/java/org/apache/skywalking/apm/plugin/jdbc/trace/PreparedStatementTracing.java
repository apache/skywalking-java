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

package org.apache.skywalking.apm.plugin.jdbc.trace;

import java.sql.SQLException;
import java.util.Objects;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.plugin.jdbc.JDBCPluginConfig;
import org.apache.skywalking.apm.plugin.jdbc.PreparedStatementParameterBuilder;
import org.apache.skywalking.apm.plugin.jdbc.define.StatementEnhanceInfos;

/**
 * {@link PreparedStatementTracing} create an exit span when the client call the method in the class that extend {@link
 * java.sql.PreparedStatement}.
 */
public class PreparedStatementTracing {

    public static <R> R execute(java.sql.PreparedStatement realStatement, ConnectionInfo connectInfo, String method,
            String sql, Executable<R> exec, StatementEnhanceInfos statementEnhanceInfos) throws SQLException {
        final AbstractSpan span = ContextManager.createExitSpan(
                connectInfo.getDBType() + "/JDBC/PreparedStatement/" + method, connectInfo
                        .getDatabasePeer());
        try {
            Tags.DB_TYPE.set(span, connectInfo.getDBType());
            Tags.DB_INSTANCE.set(span, connectInfo.getDatabaseName());
            Tags.DB_STATEMENT.set(span, sql);
            span.setComponent(connectInfo.getComponent());
            SpanLayer.asDB(span);
            if (JDBCPluginConfig.Plugin.JDBC.TRACE_SQL_PARAMETERS && Objects.nonNull(statementEnhanceInfos)) {
                final Object[] parameters = statementEnhanceInfos.getParameters();
                if (parameters != null && parameters.length > 0) {
                    int maxIndex = statementEnhanceInfos.getMaxIndex();
                    Tags.SQL_PARAMETERS.set(span, getParameterString(parameters, maxIndex));
                }
            }
            return exec.exe(realStatement, sql);
        } catch (SQLException e) {
            span.log(e);
            throw e;
        } finally {
            ContextManager.stopSpan(span);
        }
    }

    private static String getParameterString(Object[] parameters, int maxIndex) {
        return new PreparedStatementParameterBuilder()
                .setParameters(parameters)
                .setMaxIndex(maxIndex)
                .build();
    }

    public interface Executable<R> {

        R exe(java.sql.PreparedStatement realConnection, String sql) throws SQLException;
    }
}
