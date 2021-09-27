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

package org.apache.skywalking.apm.plugin.jackson.define;

import com.google.common.collect.ImmutableMap;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import java.util.Map;

/**
 * Jackson provides a "one stop" solution for json serialization and deserialization solution
 * basic requirements.
 * <p>
 * ObjectWriter: writeValue()\writeValueAsString()\writeValueAsBytes()
 */

public class ObjectWriterInstrumentation extends AbstractInstrumentation {

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName("com.fasterxml.jackson.databind.ObjectWriter");
    }

    @Override
    protected Map<String, String> enhanceMethods() {
        return ImmutableMap.<String, String>builder()
                .put(
                        "writeValue",
                        "org.apache.skywalking.apm.plugin.jackson.BasicMethodsInterceptor"
                )
                .put(
                        "writeValueAsString",
                        "org.apache.skywalking.apm.plugin.jackson.WriteValueAsStringInterceptor"
                )
                .put(
                        "writeValueAsBytes",
                        "org.apache.skywalking.apm.plugin.jackson.WriteValueAsBytesInterceptor"
                )
                .build();
    }
}
