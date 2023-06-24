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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Log {
    private static ByteArrayOutputStream OUTPUT;
    private static ByteArrayOutputStream ERR_OUTPUT;
    private static PrintStream PRINT;
    private static PrintStream ERROR_PRINT;

    public static void info(String msg, Object... args) {
        msg = formatLog(msg, args);
        PRINT.println(msg);
    }

    public static void error(String msg, Object... args) {
        msg = formatLog(msg, args);
        ERROR_PRINT.println(msg);
    }

    private static String formatLog(String msg, Object[] args) {
        msg = msg.replaceAll("\\{}", "%s");
        msg = String.format(msg, args);
        return msg;
    }

    public static void clear() {
        OUTPUT = new ByteArrayOutputStream(128);
        ERR_OUTPUT = new ByteArrayOutputStream(128);
        PRINT = new PrintStream(OUTPUT, true);
        ERROR_PRINT = new PrintStream(ERR_OUTPUT, true);
    }

    public static void printToConsole() {
        PrintStream out = System.out;
        PrintStream err = System.err;
        out.println(OUTPUT.toString());
        err.println(ERR_OUTPUT.toString());
    }
}
