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

import java.io.PrintStream;

public class Log {
    private static PrintStream PRINT = System.out;
    private static PrintStream ERROR_PRINT = System.err;

    public static void info(String msg) {
        PRINT.println(msg);
    }

    public static void info(int msg) {
        PRINT.println(msg);
    }

    public static void info(Object obj) {
        PRINT.println(obj);
    }

    public static void error(String msg) {
        ERROR_PRINT.println(msg);
    }
}
