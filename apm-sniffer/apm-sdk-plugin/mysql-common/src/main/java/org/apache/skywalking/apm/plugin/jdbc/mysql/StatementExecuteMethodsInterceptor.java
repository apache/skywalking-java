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

package org.apache.skywalking.apm.plugin.jdbc.mysql;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.jdbc.SqlBodyUtil;
import org.apache.skywalking.apm.plugin.jdbc.define.StatementEnhanceInfos;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.apache.skywalking.apm.util.StringUtil;

import java.lang.reflect.Method;

public class StatementExecuteMethodsInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public final void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        StatementEnhanceInfos cacheObject = (StatementEnhanceInfos) objInst.getSkyWalkingDynamicField();
        ConnectionInfo connectInfo = cacheObject.getConnectionInfo();
        /**
         * To protected the code occur NullPointException. because mysql execute system sql when constructor method in
         * {@link com.mysql.jdbc.ConnectionImpl} class executed. but the interceptor set the connection Info after
         * the constructor method executed.
         *
         * @see JDBCDriverInterceptor#afterMethod(EnhancedInstance, Method, Object[], Class[], Object)
         */
        if (connectInfo != null) {

            AbstractSpan span = ContextManager.createExitSpan(buildOperationName(connectInfo, method.getName(), cacheObject
                .getStatementName()), connectInfo.getDatabasePeer());
            Tags.DB_TYPE.set(span, connectInfo.getDBType());
            Tags.DB_INSTANCE.set(span, connectInfo.getDatabaseName());

            /**
             * Except for the `executeBatch` method, the first parameter of all enhanced methods in `com.mysql.jdbc.StatementImpl` is the SQL statement.
             * Therefore, executeBatch will attempt to obtain the SQL from `cacheObject`.
             */
            String sql = "";
            if (allArguments.length > 0) {
                sql = (String) allArguments[0];
                sql = SqlBodyUtil.limitSqlBodySize(sql);
            } else if (StringUtil.isNotBlank(cacheObject.getSql())) {
                sql = SqlBodyUtil.limitSqlBodySize(cacheObject.getSql());
            }

            Tags.DB_STATEMENT.set(span, sql);
            span.setComponent(connectInfo.getComponent());

            SpanLayer.asDB(span);
        }
    }

    @Override
    public final Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        StatementEnhanceInfos cacheObject = (StatementEnhanceInfos) objInst.getSkyWalkingDynamicField();
        if (cacheObject.getConnectionInfo() != null) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public final void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        StatementEnhanceInfos cacheObject = (StatementEnhanceInfos) objInst.getSkyWalkingDynamicField();
        if (cacheObject.getConnectionInfo() != null) {
            ContextManager.activeSpan().log(t);
        }
    }

    private String buildOperationName(ConnectionInfo connectionInfo, String methodName, String statementName) {
        return connectionInfo.getDBType() + "/JDBC/" + statementName + "/" + methodName;
    }
}
