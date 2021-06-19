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

package org.apache.skywalking.apm.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Group patterns use {@link java.util.regex.Pattern} as core, could group the input strings to matched group or return
 * original string.
 */
@ToString
public class StringFormatGroup {
    private final List<PatternRule> rules;

    public StringFormatGroup() {
        rules = new ArrayList<>();
    }

    /**
     * Add a new match rule. The rule will follow the order of being added.
     *
     * @param name      will be used when ruleRegex matched.
     * @param ruleRegex to match target string.
     */
    public void addRule(String name, String ruleRegex) {
        for (PatternRule rule : rules) {
            if (rule.name.equals(name)) {
                return;
            }
        }
        PatternRule rule = new PatternRule(name, ruleRegex);
        rules.add(rule);
    }

    /**
     * Format the string based on rules.
     *
     * @param string to be formatted
     * @return matched rule name, or original string.
     */
    public FormatResult format(String string) {
        for (PatternRule rule : rules) {
            if (rule.getPattern().matcher(string).matches()) {
                return new FormatResult(true, rule.getName(), string);
            }
        }
        return new FormatResult(false, string, string);
    }

    public void sortRules(Comparator<? super PatternRule> comparator) {
        rules.sort(comparator);
    }

    @Getter
    @RequiredArgsConstructor
    public static class FormatResult {
        private final boolean match;
        private final String name;
        private final String replacedName;
    }

    @Getter
    @ToString
    public static class PatternRule {
        private final String name;
        private final Pattern pattern;

        private PatternRule(String name, String ruleRegex) {
            this.name = name;
            pattern = Pattern.compile(ruleRegex);
        }
    }
}
