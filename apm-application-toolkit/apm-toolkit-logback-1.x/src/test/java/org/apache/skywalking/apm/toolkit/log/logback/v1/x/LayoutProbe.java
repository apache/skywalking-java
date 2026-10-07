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
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout;

/**
 * Runs inside the class loader of the logback release under test, so it only uses logback APIs that exist from
 * 1.2 to 1.6.
 */
public final class LayoutProbe {

    private LayoutProbe() {
    }

    /**
     * Starts both SkyWalking layouts in one context, as a logback.xml using both of them does, and renders an event
     * with each.
     */
    public static String[] render(String tidPattern, String mdcPattern) {
        LoggerContext context = new LoggerContext();
        PatternLayout tidLayout = start(new TraceIdPatternLogbackLayout(), context, tidPattern);
        PatternLayout mdcLayout = start(new TraceIdMDCPatternLogbackLayout(), context, mdcPattern);

        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        return new String[] {
            tidLayout.doLayout(event),
            mdcLayout.doLayout(event)
        };
    }

    private static PatternLayout start(PatternLayout layout, LoggerContext context, String pattern) {
        layout.setContext(context);
        layout.setPattern(pattern);
        layout.start();
        if (!layout.isStarted()) {
            throw new IllegalStateException(layout.getClass().getSimpleName() + " failed to start");
        }
        return layout;
    }
}
