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

package org.apache.skywalking.apm.toolkit.log.log4j.v2.x;

public class Log4j2SkyWalkingContextOutputAppender {
    /**
     * As default, append "SW_CTX: N/A" to the output message, if SkyWalking agent in active mode, append the real SkyWalking context
     * in the recent Context, if existed, or empty String.
     *
     * @param toAppendTo origin output message.
     */
    public static void append(StringBuilder toAppendTo) {
        toAppendTo.append("SW_CTX: N/A");
    }
}
