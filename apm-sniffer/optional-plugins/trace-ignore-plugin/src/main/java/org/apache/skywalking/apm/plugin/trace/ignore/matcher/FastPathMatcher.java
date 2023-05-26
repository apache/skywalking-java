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

package org.apache.skywalking.apm.plugin.trace.ignore.matcher;

public class FastPathMatcher implements TracePathMatcher {
    @Override
    public boolean match(String pattern, String path) {
        return normalMatch(pattern, 0, path, 0);
    }

    private boolean normalMatch(String pat, int p, String str, int s) {
        while (p < pat.length()) {
            char pc = pat.charAt(p);
            char sc = safeCharAt(str, s);

            // Got * in pattern, enter the wildcard mode.
            //            ↓        ↓
            // pattern: a/*      a/*
            //            ↓        ↓
            // string:  a/bcd    a/
            if (pc == '*') {
                p++;
                // Got * in pattern again, enter the multi-wildcard mode.
                //             ↓        ↓
                // pattern: a/**     a/**
                //            ↓        ↓
                // string:  a/bcd    a/
                if (safeCharAt(pat, p) == '*') {
                    p++;
                    // Enter the multi-wildcard mode.
                    //              ↓        ↓
                    // pattern: a/**     a/**
                    //            ↓        ↓
                    // string:  a/bcd    a/
                    return multiWildcardMatch(pat, p, str, s);
                } else {
                    // Enter the wildcard mode.
                    //             ↓
                    // pattern: a/*
                    //            ↓
                    // string:  a/bcd
                    return wildcardMatch(pat, p, str, s);
                }
            }

            // Matching ? for non-'/' char, or matching the same chars.
            //            ↓        ↓       ↓
            // pattern: a/?/c    a/b/c    a/b
            //            ↓        ↓       ↓
            // string:  a/b/c    a/b/d    a/d
            if (pc == '?' && sc != 0 && sc != '/' || pc == sc) {
                s++;
                p++;
                continue;
            }

            // Not matched.
            //            ↓
            // pattern: a/b
            //            ↓
            // string:  a/c
            return false;
        }

        return s == str.length();
    }

    private boolean wildcardMatch(String pat, int p, String str, int s) {
        char pc = safeCharAt(pat, p);

        // End of pattern, check string only has zero or one '/' at end.
        //             ↓        ↓
        // pattern: a/*      a/*
        //            ↓        ↓
        // string:  a/bc/    a/bc
        if (pc == 0) {
            while (true) {
                char sc = safeCharAt(str, s);
                // No '/' found
                if (sc == 0) return true;
                // Check '/' is the last char of string
                if (sc == '/') return s == str.length() - 1;
                s++;
            }
        }

        while (true) {
            char sc = safeCharAt(str, s);

            if (sc == '/') {
                // Both of pattern and string '/' matched, exit wildcard mode.
                //             ↓
                // pattern: a/*/
                //              ↓
                // string:  a/bc/
                if (pc == sc) {
                    return normalMatch(pat, p + 1, str, s + 1);
                }

                // Not matched string in current path part.
                //             ↓        ↓
                // pattern: a/*      a/*d
                //              ↓        ↓
                // string:  a/bc/    a/bc/
                return false;
            }

            // Try to enter normal mode, if not matched, increasing pointer of string and try again.
            if (!normalMatch(pat, p, str, s)) {
                // End of string, not matched.
                if (s >= str.length()) {
                    return false;
                }

                s++;
                continue;
            }

            // Matched in next normal mode.
            return true;
        }
    }

    private boolean multiWildcardMatch(String pat, int p, String str, int s) {
        switch (safeCharAt(pat, p)) {
            // End of pattern, just return true quickly.
            case 0: return true;
            // Skip next '/' for pattern to match zero path part.
            case '/': p++;
        }

        while (true) {
            // Try to enter normal mode, if not matched, increasing pointer of string and try again.
            if (!normalMatch(pat, p, str, s)) {
                // End of string, not matched.
                if (s >= str.length()) {
                    return false;
                }

                s++;
                continue;
            }

            return true;
        }
    }

    private char safeCharAt(String value, int index) {
        if (index >= value.length()) {
            return 0;
        }

        return value.charAt(index);
    }
}
