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

import java.util.ArrayList;

public class Utils {
    private static final Class<?> DISPATCHED_TASK_CLASS;

    static {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(DispatchedTaskInstrumentation.ENHANCE_CLASS);
        } catch (ClassNotFoundException ignored) {
        }
        DISPATCHED_TASK_CLASS = clazz;
    }

    public static boolean isDispatchedTask(Runnable runnable) {
        return DISPATCHED_TASK_CLASS.isAssignableFrom(runnable.getClass());
    }

    public static String[] getStackTraceElements(Runnable runnable) {
        ArrayList<String> elements = new ArrayList<>();
        if (runnable instanceof CoroutineStackFrame) {
            CoroutineStackFrame frame = (CoroutineStackFrame) runnable;
            while (frame != null) {
                StackTraceElement element = frame.getStackTraceElement();
                if (element != null) {
                    elements.add(element.toString());
                } else {
                    elements.add("Unknown Source");
                }
                frame = frame.getCallerFrame();
            }
            return elements.toArray(new String[0]);
        }
        return null;
    }
}
