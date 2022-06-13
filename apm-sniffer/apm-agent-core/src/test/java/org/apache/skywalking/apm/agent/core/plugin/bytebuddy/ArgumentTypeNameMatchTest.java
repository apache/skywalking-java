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

package org.apache.skywalking.apm.agent.core.plugin.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Test;

public class ArgumentTypeNameMatchTest {

    @Test
    public void testMatch() throws Exception {
        final ElementMatcher<MethodDescription> matcherPerson = ArgumentTypeNameMatch.takesArgumentWithType(0, "org.apache.skywalking.apm.agent.core.plugin.bytebuddy.Person");
        Assert.assertTrue(matcherPerson.matches(new MethodDescription.ForLoadedMethod(Person.class.getMethod("isOlderThan", Person.class))));
        Assert.assertFalse(matcherPerson.matches(new MethodDescription.ForLoadedMethod(Person.class.getMethod("isMemberOf", Person[].class))));

        final ElementMatcher<MethodDescription> matcherPersonArray = ArgumentTypeNameMatch.takesArgumentWithType(0, "[Lorg.apache.skywalking.apm.agent.core.plugin.bytebuddy.Person;");
        Assert.assertTrue(matcherPersonArray.matches(new MethodDescription.ForLoadedMethod(Person.class.getMethod("isMemberOf", Person[].class))));
        Assert.assertFalse(matcherPersonArray.matches(new MethodDescription.ForLoadedMethod(Person.class.getMethod("isOlderThan", Person.class))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMatchWithException() {
        ArgumentTypeNameMatch.takesArgumentWithType(0, "org.apache.skywalking.apm.agent.core.plugin.bytebuddy.Person[]");
    }

}