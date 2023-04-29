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

package net.bytebuddy.agent.builder;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.utility.RandomString;

/**
 * Generate fixed origin method name
 */
public class SWMethodNameTransformer implements MethodNameTransformer {

    private static final String DEFAULT_PREFIX = "original$";

    private String prefix;

    public SWMethodNameTransformer() {
        this(DEFAULT_PREFIX);
    }

    public SWMethodNameTransformer(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String transform(MethodDescription methodDescription) {
        return prefix + methodDescription.getInternalName() + "$" + RandomString.hashOf(methodDescription.toString().hashCode());
    }

}
