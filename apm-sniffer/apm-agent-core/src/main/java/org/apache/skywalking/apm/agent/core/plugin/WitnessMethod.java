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

package org.apache.skywalking.apm.agent.core.plugin;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Witness Method for plugin activation
 */
@ToString
public class WitnessMethod {

    /**
     * the class or interface name where the witness method is declared.
     */
    @Getter
    private final String declaringClassName;
    /**
     * matcher to match the witness method
     */
    @Getter
    private final ElementMatcher<? super MethodDescription.InDefinedShape> elementMatcher;

    public WitnessMethod(String declaringClassName, ElementMatcher.Junction<? super MethodDescription.InDefinedShape> elementMatcher) {
        this.declaringClassName = declaringClassName;
        this.elementMatcher = Preconditions.checkNotNull(elementMatcher).and(not(isSynthetic()));
    }
}
