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

package org.apache.skywalking.apm.agent.core.boot;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

/**
 * this test case is to verify the fix of bug
 * https://github.com/apache/skywalking/issues/10770
 */
public class AgentPackagePathTest {

    @Test
    public void testGetPathNotNull() throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, IOException {
        String classpath = System.getProperties().getProperty("java.class.path");
        //to find the location of apm-agent-test java compiled path
        String[] classpathArray = classpath.split(File.pathSeparator);
        String currentClassPath = null;
        for (String path : classpathArray) {
            if (path.contains("apm-agent-test" + File.separator + "target" + File.separator + "test-classes")) {
                currentClassPath = path;
                break;
            }
        }
        // it must exist
        if (currentClassPath == null) {
            throw new RuntimeException("apm-agent-test path not found,current classpath: "+classpath);
        }
        //base on currentClassPath, find apm-agent-core java compiled path
        String apmAgentCoreClassPath = currentClassPath
                .replace("apm-agent-test", "apm-agent-core")
                .replace("test-classes", "classes");

        File f = new File(apmAgentCoreClassPath);
        URI u = f.toURI();

        //add apmAgentCoreClassPath to URLClassLoader
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{u.toURL()});

        //use this classloader to load AgentPackagePath
        //simulate org.jboss.modules.Main.main() to load AgentJar
        Class<?> agentPackagePath = (Class<?>) Class.forName("org.apache.skywalking.apm.agent.core.boot.AgentPackagePath", true, urlClassLoader);

        Method getPath = agentPackagePath.getMethod("getPath");
        File path = (File) getPath.invoke(null);
        assert path != null;
    }
}
