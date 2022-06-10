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

package org.apache.skywalking.apm.plugin.jdbc.postgresql.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.jdbc.PgConnection;

import java.lang.reflect.Array;

public class ConnectionInstrumentationTest {
    private ConnectionInstrumentation connectionInstrumentation;

    @Before
    public void setUp() {
        connectionInstrumentation = new ConnectionInstrumentation();
    }

    @Test
    public void testMethodMatch() throws Exception {
        final InstanceMethodsInterceptPoint[] interceptPoints = connectionInstrumentation.getInstanceMethodsInterceptPoints();
        Assert.assertEquals(5, interceptPoints.length);
        final ElementMatcher<MethodDescription> arrayArgumentMatcher = interceptPoints[1].getMethodsMatcher();
        final MethodDescription method = new MethodDescription.ForLoadedMethod(PgConnection.class.getMethod("prepareStatement", String.class, Array.newInstance(String.class, 0).getClass()));
        Assert.assertTrue(arrayArgumentMatcher.matches(method));
    }
}
