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

package org.apache.skywalking.apm.toolkit.log.logback.v1.x;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Renders the layouts against each supported logback release. The jars are copied by maven-dependency-plugin, and
 * each release gets its own class loader with the toolkit classes, so no other logback version is visible.
 */
@RunWith(Parameterized.class)
public class LogbackVersionCompatibilityTest {

    @Parameterized.Parameters(name = "logback {0}")
    public static Collection<Object[]> versions() {
        return Arrays.asList(new Object[][] {
            {"1.2.13", "1.7.36"},
            {"1.3.16", "2.0.19"},
            {"1.4.14", "2.0.19"},
            // The last release reading class names from the context registry directly.
            {"1.5.12", "2.0.19"},
            // The first release adapting class names in the context registry into converter suppliers.
            {"1.5.14", "2.0.19"},
            {"1.5.38", "2.0.19"},
            // PatternLayout.defaultConverterMap removed since 1.6.0.
            {"1.6.5", "2.0.19"}
        });
    }

    private final String logbackVersion;
    private final String slf4jVersion;

    public LogbackVersionCompatibilityTest(String logbackVersion, String slf4jVersion) {
        this.logbackVersion = logbackVersion;
        this.slf4jVersion = slf4jVersion;
    }

    @Test
    public void rendersSkyWalkingConversionWords() throws Exception {
        try (URLClassLoader classLoader = new URLClassLoader(classpath(), ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> probe = Class.forName(LayoutProbe.class.getName(), true, classLoader);
            String[] rendered = (String[]) invoke(probe, "%tid|%sw_ctx", "%X{tid}|%mdc{sw_ctx}");

            assertArrayEquals(new String[] {
                "TID: N/A|SW_CTX: N/A",
                "TID: N/A|SW_CTX: N/A"
            }, rendered);
        }
    }

    private URL[] classpath() throws Exception {
        File dir = new File(System.getProperty("logback.compat.dir", "target/logback-compat"));
        File[] jars = {
            new File(dir, "logback-core-" + logbackVersion + ".jar"),
            new File(dir, "logback-classic-" + logbackVersion + ".jar"),
            new File(dir, "slf4j-api-" + slf4jVersion + ".jar")
        };
        URL[] urls = new URL[jars.length + 2];
        for (int i = 0; i < jars.length; i++) {
            assertTrue(jars[i] + " is missing, it is copied by maven-dependency-plugin in generate-test-resources",
                       jars[i].isFile());
            urls[i] = jars[i].toURI().toURL();
        }
        urls[jars.length] = AbstractTraceIdPatternLogbackLayout.class.getProtectionDomain().getCodeSource().getLocation();
        urls[jars.length + 1] = LayoutProbe.class.getProtectionDomain().getCodeSource().getLocation();
        return urls;
    }

    private static Object invoke(Class<?> probe, String tidPattern, String mdcPattern) throws Exception {
        try {
            return probe.getMethod("render", String.class, String.class).invoke(null, tidPattern, mdcPattern);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }
}
