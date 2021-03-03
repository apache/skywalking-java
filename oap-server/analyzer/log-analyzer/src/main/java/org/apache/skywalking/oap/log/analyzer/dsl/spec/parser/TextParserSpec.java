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

package org.apache.skywalking.oap.log.analyzer.dsl.spec.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.skywalking.apm.network.logging.v3.LogData;
import org.apache.skywalking.oap.log.analyzer.provider.LogAnalyzerModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

public class TextParserSpec extends AbstractParserSpec {
    public TextParserSpec(final ModuleManager moduleManager,
                          final LogAnalyzerModuleConfig moduleConfig) {
        super(moduleManager, moduleConfig);
    }

    @SuppressWarnings("unused")
    public boolean regexp(final String regexp) {
        return regexp(Pattern.compile(regexp));
    }

    public boolean regexp(final Pattern pattern) {
        if (BINDING.get().shouldAbort()) {
            return false;
        }
        final LogData.Builder log = BINDING.get().log();
        final Matcher matcher = pattern.matcher(log.getBody().getText().getText());
        final boolean matched = matcher.find();
        if (matched) {
            BINDING.get().parsed(matcher);
        } else if (abortOnFailure()) {
            BINDING.get().abort();
        }
        return matched;
    }

    public boolean grok(final String grok) {
        // TODO
        return false;
    }

}
