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

package org.apache.skywalking.apm.agent.bytebuddy;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.Arrays;

public class InstMethodAdvice {

    @Advice.OnMethodEnter(inline = false)
    public static void onEnter(@Advice.This Object target,
                               @Advice.Origin Method method,
                               @Advice.AllArguments Object[] args) throws Exception {
        Log.info(String.format("InstMethodAdvice.onEnter: constructor: %s, args: %s", method, Arrays.asList(args)));
    }

    @Advice.OnMethodExit(inline = false)
    public static void onExit(@Advice.This Object target,
                              @Advice.Origin Method method,
                              @Advice.AllArguments Object[] args) throws Exception {
        Log.info(String.format("InstMethodAdvice.onExit: constructor: %s, args: %s", method, Arrays.asList(args)));
    }

}