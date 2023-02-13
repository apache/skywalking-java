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

import java.sql.SQLException;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

/**
 *
 */
public class ClickHouseStatementTracingWrapper {

    public static <T> T of(ConnectionInfo connectionInfo, String methodName, String sql,
            SupplierWithException<T> supplier) throws SQLException {
        final AbstractSpan span = ContextManager.createExitSpan(
                connectionInfo.getDBType() + "/JDBC/Statement/" + methodName, connectionInfo.getDatabasePeer());
        try {
            Tags.DB_TYPE.set(span, connectionInfo.getDBType());
            Tags.DB_INSTANCE.set(span, connectionInfo.getDatabaseName());
            Tags.DB_STATEMENT.set(span, sql);
            span.setComponent(connectionInfo.getComponent());
            SpanLayer.asDB(span);
            return supplier.get();
        } catch (SQLException e) {
            span.log(e);
            throw e;
        } finally {
            ContextManager.stopSpan();
        }
    }

    public interface SupplierWithException<T> {

        T get() throws SQLException;
    }

}
