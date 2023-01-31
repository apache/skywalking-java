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

package org.apache.skywalking.apm.plugin.jdbc.mysql.v5;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionImplCreateInterceptorTest {
    private ConnectionCreate5xInterceptor interceptor;

    @Mock
    private EnhancedInstance objectInstance;

    @Before
    public void setUp() {
        interceptor = new ConnectionCreate5xInterceptor();
    }

    @Test
    public void testResultIsEnhanceInstance() throws Throwable {
        interceptor.afterMethod(null, null, new Object[] {
            "localhost",
            3360,
            null,
            "test",
            "jdbc:mysql:replication://localhost:3360,localhost:3360,localhost:3360/test?useUnicode=true&characterEncoding=utf8&useSSL=false&roundRobinLoadBalance=true"
        }, null, objectInstance);
        verify(objectInstance).setSkyWalkingDynamicField(any());
    }

    @Test
    public void testResultIsEnhanceInstanceWithNoDatabase() throws Throwable {
        interceptor.afterMethod(null, null, new Object[] {
                "localhost",
                3360,
                null,
                null,
                "jdbc:mysql:replication://localhost:3360,localhost:3360,localhost:3360/test?useUnicode=true&characterEncoding=utf8&useSSL=false&roundRobinLoadBalance=true"
        }, null, objectInstance);
        verify(objectInstance).setSkyWalkingDynamicField(any());
    }
}
