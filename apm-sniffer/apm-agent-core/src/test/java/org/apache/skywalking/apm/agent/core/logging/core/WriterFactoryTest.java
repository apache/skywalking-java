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

package org.apache.skywalking.apm.agent.core.logging.core;

import static org.junit.Assert.assertTrue;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.conf.SnifferConfigInitializer;
import org.apache.skywalking.apm.agent.core.plugin.PluginFinder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WriterFactoryTest {
    private MockedStatic<SnifferConfigInitializer> mockedSnifferConfigInitializer =
        Mockito.mockStatic(SnifferConfigInitializer.class);
    private MockedStatic<PluginFinder> mockedPluginFinder =
        Mockito.mockStatic(PluginFinder.class);
    private MockedStatic<AgentPackagePath> mockedAgentPackagePath =
        Mockito.mockStatic(AgentPackagePath.class);

    @After
    public void tearDown() {
        mockedSnifferConfigInitializer.close();
        mockedPluginFinder.close();
        mockedAgentPackagePath.close();
    }

    @Test
    public void alwaysReturnSystemLogWriteWithSetLoggingDir() {
        Config.Logging.OUTPUT = LogOutput.CONSOLE;

        mockedSnifferConfigInitializer.when(SnifferConfigInitializer::isInitCompleted).thenReturn(true);
        mockedPluginFinder.when(PluginFinder::isPluginInitCompleted).thenReturn(true);
        mockedAgentPackagePath.when(AgentPackagePath::isPathFound).thenReturn(true);

        assertTrue(SnifferConfigInitializer.isInitCompleted());
        assertTrue(PluginFinder.isPluginInitCompleted());
        assertTrue(AgentPackagePath.isPathFound());

        IWriter logWriter = WriterFactory.getLogWriter();
        assertTrue(logWriter instanceof SystemOutWriter);
    }

    @Test
    public void returnFileWriterWriteWithBlankLoggingDir() {
        Config.Logging.OUTPUT = LogOutput.FILE;
        mockedSnifferConfigInitializer.when(SnifferConfigInitializer::isInitCompleted).thenReturn(true);
        mockedPluginFinder.when(PluginFinder::isPluginInitCompleted).thenReturn(true);
        mockedAgentPackagePath.when(AgentPackagePath::isPathFound).thenReturn(true);

        assertTrue(SnifferConfigInitializer.isInitCompleted());
        assertTrue(PluginFinder.isPluginInitCompleted());
        assertTrue(AgentPackagePath.isPathFound());

        IWriter logWriter = WriterFactory.getLogWriter();
        assertTrue(logWriter instanceof FileWriter);
    }
}
