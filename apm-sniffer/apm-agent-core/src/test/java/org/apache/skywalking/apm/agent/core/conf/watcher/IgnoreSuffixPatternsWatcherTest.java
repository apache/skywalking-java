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

package org.apache.skywalking.apm.agent.core.conf.watcher;

import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.dynamic.AgentConfigChangeWatcher;
import org.apache.skywalking.apm.agent.core.conf.dynamic.watcher.IgnoreSuffixPatternsWatcher;
import org.apache.skywalking.apm.agent.core.context.ContextManagerExtendService;
import org.apache.skywalking.apm.agent.core.test.tools.AgentServiceRule;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

public class IgnoreSuffixPatternsWatcherTest {

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    private ContextManagerExtendService contextManagerExtendService;

    @Before
    public void setUp() {
        contextManagerExtendService = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);
    }

    @AfterClass
    public static void afterClass() {
        ServiceManager.INSTANCE.shutdown();
    }

    @Test
    public void testConfigModifyEvent() {
        IgnoreSuffixPatternsWatcher ignoreSuffixPatternsWatcher = Whitebox.getInternalState(contextManagerExtendService
            , "ignoreSuffixPatternsWatcher");
        ignoreSuffixPatternsWatcher.notify(new AgentConfigChangeWatcher.ConfigChangeEvent(
            ".txt,.log",
            AgentConfigChangeWatcher.EventType.MODIFY
        ));
        Assert.assertEquals(".txt,.log", ignoreSuffixPatternsWatcher.getIgnoreSuffixPatterns());
        Assert.assertEquals("agent.ignore_suffix", ignoreSuffixPatternsWatcher.getPropertyKey());
    }

    @Test
    public void testConfigDeleteEvent() {
        IgnoreSuffixPatternsWatcher ignoreSuffixPatternsWatcher = Whitebox.getInternalState(contextManagerExtendService
            , "ignoreSuffixPatternsWatcher");
        ignoreSuffixPatternsWatcher.notify(new AgentConfigChangeWatcher.ConfigChangeEvent(
            null,
            AgentConfigChangeWatcher.EventType.DELETE
        ));
        Assert.assertEquals("agent.ignore_suffix", ignoreSuffixPatternsWatcher.getPropertyKey());
    }
}
