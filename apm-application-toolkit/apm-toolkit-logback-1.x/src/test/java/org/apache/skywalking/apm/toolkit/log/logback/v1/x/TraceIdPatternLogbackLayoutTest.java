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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.CoreConstants;
import java.util.HashMap;
import java.util.Map;
import org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class TraceIdPatternLogbackLayoutTest {

    @Test
    public void existingRulesKeepPrecedence() {
        LoggerContext context = new LoggerContext();
        Map<String, String> userRules = new HashMap<>();
        userRules.put("tid", FixedConverter.class.getName());
        userRules.put("user", FixedConverter.class.getName());
        context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, userRules);

        TraceIdPatternLogbackLayout layout = new TraceIdPatternLogbackLayout();
        layout.setContext(context);
        layout.setPattern("%tid|%sw_ctx|%user");
        layout.start();

        assertEquals("fixed|SW_CTX: N/A|fixed", layout.doLayout(event()));
        // Updated in place, so a rule logback adds to the same registry concurrently is kept.
        assertSame(userRules, context.getObject(CoreConstants.PATTERN_RULE_REGISTRY));
        assertEquals(LogbackSkyWalkingContextPatternConverter.class.getName(), userRules.get("sw_ctx"));
    }

    @Test
    public void layoutsInOneContextKeepEachOthersRules() {
        LoggerContext context = new LoggerContext();
        start(new TraceIdPatternLogbackLayout(), context);
        start(new TraceIdMDCPatternLogbackLayout(), context);

        @SuppressWarnings("unchecked")
        Map<String, String> rules = (Map<String, String>) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        assertEquals(LogbackPatternConverter.class.getName(), rules.get("tid"));
        assertEquals(LogbackSkyWalkingContextPatternConverter.class.getName(), rules.get("sw_ctx"));
        assertEquals(4, rules.size());
    }

    @Test
    public void notStartedWithoutContext() {
        TraceIdPatternLogbackLayout layout = new TraceIdPatternLogbackLayout();
        layout.setPattern("%tid");
        layout.start();

        assertFalse(layout.isStarted());
    }

    private static void start(AbstractTraceIdPatternLogbackLayout layout, LoggerContext context) {
        layout.setContext(context);
        layout.setPattern("%msg");
        layout.start();
    }

    private static ILoggingEvent event() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        return event;
    }

    public static class FixedConverter extends ClassicConverter {
        @Override
        public String convert(ILoggingEvent event) {
            return "fixed";
        }
    }
}
