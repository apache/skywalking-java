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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

@RunWith(PowerMockRunner.class)
public class InitConnectionMethodInterceptorTest {

    private final EnhancedInstance enhancedInstance = new EnhancedInstance() {
        private Object value;

        @Override
        public Object getSkyWalkingDynamicField() {
            return value;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.value = value;
        }
    };
    private InitConnectionMethodInterceptor targetInterceptor;
    @Mock
    private ClickHouseProperties clickHouseProperties;

    @Before
    public void setUp() throws Exception {
        targetInterceptor = new InitConnectionMethodInterceptor();
        when(clickHouseProperties.getHost()).thenReturn("127.0.0.1");
        when(clickHouseProperties.getPort()).thenReturn(8123);
        when(clickHouseProperties.getDatabase()).thenReturn("default");
    }

    @Test
    public void test() throws Throwable {
        targetInterceptor.beforeMethod(enhancedInstance, null, new Object[] {clickHouseProperties}, new Class[0], null);
        final ConnectionInfo connectionInfo = (ConnectionInfo) enhancedInstance.getSkyWalkingDynamicField();
        assertNotNull(connectionInfo);
        assertEquals("ClickHouse-jdbc-driver", connectionInfo.getComponent().getName());
        assertEquals("127.0.0.1:8123", connectionInfo.getDatabasePeer());
        assertEquals("default", connectionInfo.getDatabaseName());
        assertEquals("ClickHouse", connectionInfo.getDBType());
    }
}