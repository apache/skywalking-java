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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers the SkyWalking conversion words into the logging context before the pattern is compiled.
 * <p>
 * The context rule registry, keyed by {@link CoreConstants#PATTERN_RULE_REGISTRY} and holding converter class
 * names, is read by every logback release from 1.2 to 1.6. Logback 1.5.14+ adapts the class names into converter
 * suppliers itself. The static {@code PatternLayout.defaultConverterMap} used before was removed in logback 1.6.0.
 * <p>
 * The words are registered in the context, so pattern layouts of the context started later can use them too. The
 * registry is cleared when the context is reset, and the layouts register them again when they start. The registry
 * is replaced by an updated copy, never modified, as layouts starting concurrently may be reading it. On logback
 * before 1.5.14, which also writes {@code <conversionRule>}s to this registry, a rule logback registers at the same
 * time as a layout starts in another thread may be lost.
 * <p>
 * Logback 1.5.13 is not supported: it reads this registry as a map of suppliers, which was reverted in 1.5.14.
 */
public abstract class AbstractTraceIdPatternLogbackLayout extends PatternLayout {

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        Context context = getContext();
        if (context == null) {
            addError("A logging context is required to register the SkyWalking conversion words");
            return;
        }

        Map<String, String> rules = new HashMap<>();
        registerConverters(rules);

        // Layouts starting concurrently, from any class loader, do not lose each other's words. The monitor is only
        // held for the copy, and released before logback creates and starts the converters.
        synchronized (context) {
            Map<String, String> existing = (Map<String, String>) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
            if (existing == null || !existing.keySet().containsAll(rules.keySet())) {
                Map<String, String> registry = existing == null ? new HashMap<>() : new HashMap<>(existing);
                // Rules already registered, such as user-defined <conversionRule>s, keep their precedence.
                for (Map.Entry<String, String> rule : rules.entrySet()) {
                    registry.putIfAbsent(rule.getKey(), rule.getValue());
                }
                context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
            }
        }

        super.start();
    }

    /**
     * Puts the conversion words of this layout into {@code rules}, as conversion word to converter class name.
     * Subclasses override it to change or add words. Words already registered in the context keep their precedence.
     */
    protected abstract void registerConverters(Map<String, String> rules);
}
