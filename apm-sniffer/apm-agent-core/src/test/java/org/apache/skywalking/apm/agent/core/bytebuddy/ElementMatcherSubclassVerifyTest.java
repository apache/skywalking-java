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

package org.apache.skywalking.apm.agent.core.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.DelegateNamingResolver;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Verify subclass of ElementMatcher make sure to impl 'toString()' method.
 * see {@link InstanceMethodsInterceptPoint#computeHashCode()}, {@link DelegateNamingResolver}
 */
public class ElementMatcherSubclassVerifyTest {

    // ignore nest subclass inside PluginFinder
    private final ElementMatcher<TypeDescription> ignoredClass = nameStartsWith("org.apache.skywalking.apm.agent.core.plugin.PluginFinder$");

    @Test
    public void test() throws Exception {
        List<Class<?>> matchedCLasses = new ArrayList<>();
        ClassScan classScan = new ClassScan();
        classScan.registerListener(new ClassScan.ClassScanListener() {
            @Override
            public ElementMatcher<TypeDescription> classMatch() {
                return hasSuperType(named(ElementMatcher.class.getName())).and(not(isAbstract()));
            }

            @Override
            public void notify(Class aClass) throws Exception {
                if (ignoredClass.matches(TypeDescription.ForLoadedType.of(aClass))) {
                    return;
                }
                try {
                    // make sure subclass of ElementMatcher has implement "toString" method
                    Method toStringMethod = aClass.getDeclaredMethod("toString");
                } catch (NoSuchMethodException e) {
                    matchedCLasses.add(aClass);
                }
            }
        });
        classScan.scan();

        // print matched classes
        PrintStream err = System.err;
        for (Class<?> aClass : matchedCLasses) {
            String className = aClass.getName();
            if (!className.equals(BadMatcher.class.getName())) {
                err.println("Requiring toString() method for subclass of ElementMatcher: " + className);
            }
        }
        Assert.assertEquals(1, matchedCLasses.size());
    }

    public static class BadMatcher<T> implements ElementMatcher<T> {
        @Override
        public boolean matches(T target) {
            return false;
        }
    }
}
