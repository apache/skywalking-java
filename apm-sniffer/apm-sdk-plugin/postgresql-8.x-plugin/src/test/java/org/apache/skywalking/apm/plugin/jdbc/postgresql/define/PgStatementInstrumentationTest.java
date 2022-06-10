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
import org.postgresql.jdbc.PgStatement;

import java.lang.reflect.Array;

public class PgStatementInstrumentationTest {
    private PgStatementInstrumentation pgStatementInstrumentation;

    @Before
    public void setUp() {
        pgStatementInstrumentation = new PgStatementInstrumentation();
    }

    @Test
    public void testMethodMatch() throws Exception {
        final InstanceMethodsInterceptPoint[] interceptPoints = pgStatementInstrumentation.getInstanceMethodsInterceptPoints();
        Assert.assertEquals(1, interceptPoints.length);

        final ElementMatcher<MethodDescription> arrayArgumentMatcher = interceptPoints[0].getMethodsMatcher();
        final MethodDescription methodExecuteString = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("execute", String.class));
        final MethodDescription methodExecuteStringInt = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("execute", String.class, int.class));
        final MethodDescription methodExecuteStringIntArray = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("execute", String.class, Array.newInstance(int.class, 0).getClass()));
        final MethodDescription methodExecuteStringStringArray = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("execute", String.class, Array.newInstance(String.class, 0).getClass()));

        Assert.assertTrue(arrayArgumentMatcher.matches(methodExecuteString));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteStringInt));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteStringIntArray));
        Assert.assertTrue(arrayArgumentMatcher.matches(methodExecuteStringStringArray));

        final MethodDescription methodExecuteUpdateString = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeUpdate", String.class));
        final MethodDescription methodExecuteUpdateStringInt = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeUpdate", String.class, int.class));
        final MethodDescription methodExecuteUpdateStringIntArray = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeUpdate", String.class, Array.newInstance(int.class, 0).getClass()));
        final MethodDescription methodExecuteUpdateStringStringArray = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeUpdate", String.class, Array.newInstance(String.class, 0).getClass()));
        Assert.assertTrue(arrayArgumentMatcher.matches(methodExecuteUpdateString));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteUpdateStringInt));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteUpdateStringIntArray));
        Assert.assertTrue(arrayArgumentMatcher.matches(methodExecuteUpdateStringStringArray));

        final MethodDescription methodExecuteLargeUpdateString = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeLargeUpdate", String.class));
        final MethodDescription methodExecuteLargeUpdateStringInt = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeLargeUpdate", String.class, int.class));
        final MethodDescription methodExecuteLargeUpdateStringIntArray = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeLargeUpdate", String.class, Array.newInstance(int.class, 0).getClass()));
        final MethodDescription methodExecuteLargeUpdateStringStringArray = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeLargeUpdate", String.class, Array.newInstance(String.class, 0).getClass()));
        final MethodDescription methodExecuteLargeUpdateInvalid = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeLargeUpdate"));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteLargeUpdateString));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteLargeUpdateStringInt));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteLargeUpdateStringIntArray));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteLargeUpdateStringStringArray));
        Assert.assertFalse(arrayArgumentMatcher.matches(methodExecuteLargeUpdateInvalid));

        final MethodDescription methodExecuteQuery = new MethodDescription.ForLoadedMethod(PgStatement.class.getMethod("executeQuery", String.class));
        Assert.assertTrue(arrayArgumentMatcher.matches(methodExecuteQuery));
    }
}
