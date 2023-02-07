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

package org.apache.skywalking.apm.plugin.kotlin.coroutine;

import kotlin.coroutines.jvm.internal.CoroutineStackFrame;
import org.apache.skywalking.apm.plugin.kotlin.coroutine.define.DispatchedTaskInstrumentation;

public class Utils {
    private static Class<?> DISPATCHED_TASK_CLASS;

    public static boolean isDispatchedTask(Runnable runnable) {
        if (DISPATCHED_TASK_CLASS == null) {
            try {
                DISPATCHED_TASK_CLASS = Class.forName(DispatchedTaskInstrumentation.ENHANCE_CLASS);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (DISPATCHED_TASK_CLASS == null) return false;
        return DISPATCHED_TASK_CLASS.isAssignableFrom(runnable.getClass());
    }

    public static StackTraceElement getSuspensionPoint(Runnable runnable) {
        if (!(runnable instanceof CoroutineStackFrame)) {
            return null;
        }

        CoroutineStackFrame frame = (CoroutineStackFrame) runnable;
        while (frame != null) {
            StackTraceElement element = frame.getStackTraceElement();
            frame = frame.getCallerFrame();

            if (element == null) continue;
            if (element.getClassName().startsWith("kotlinx.coroutines")) continue;
            if (element.getClassName().startsWith("kotlin.coroutines")) continue;
            return element;
        }
        return null;
    }
}
