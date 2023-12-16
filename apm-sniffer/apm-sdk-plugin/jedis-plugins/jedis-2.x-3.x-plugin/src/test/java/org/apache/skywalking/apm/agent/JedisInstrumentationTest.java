/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.agent;

import com.google.common.base.Stopwatch;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.skywalking.apm.agent.core.logging.core.SystemOutWriter;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.ByteBuddyCoreClasses;
import org.apache.skywalking.apm.agent.core.plugin.PluginFinder;
import org.apache.skywalking.apm.plugin.jedis.v3.define.JedisInstrumentation;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JedisInstrumentationTest {

    @Test
    public void test() throws Exception {
        // tested plugins
        List<AbstractClassEnhancePluginDefine> plugins = Arrays.asList(new JedisInstrumentation());

        // remove shade prefix
        String[] classes = ByteBuddyCoreClasses.CLASSES;
        for (int i = 0; i < classes.length; i++) {
            classes[i] = classes[i].replaceFirst("org.apache.skywalking.apm.dependencies.", "");
        }

        Instrumentation instrumentation = ByteBuddyAgent.install();
        SkyWalkingAgent.installClassTransformer(instrumentation, new PluginFinder(plugins));

        // first load
        Jedis jedis = new Jedis();
        try {
            jedis.get("mykey");
        } catch (Exception e) {
            Assert.assertTrue(e.toString(), e.toString().contains("JedisConnectionException"));
        }

        log("Do re-transform class : redis.clients.jedis.Jedis ..");
        Stopwatch stopwatch = Stopwatch.createStarted();

        // re-transform class
        for (int i = 0; i < 4; i++) {
            stopwatch.reset();
            stopwatch.start();
            instrumentation.retransformClasses(Jedis.class);
            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            log("Re-transform class cost: " + elapsed);
        }

        // test after re-transform class
        try {
            jedis.get("mykey");
        } catch (Exception e) {
            Assert.assertTrue(e.toString(), e.toString().contains("JedisConnectionException"));
        }
    }

    private void log(String message) {
        SystemOutWriter.INSTANCE.write(message);
    }
}
