package org.apache.skywalking.apm.toolkit.trace.msg;
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


public class TraceMsgContext {


    /**
     * Message business logic processing success.
     *
     * @param trace the Context Trace information for the message association
     */
    public static void ConsumerMsgSucceed(String trace) {

    }


    /**
     * Message business logic processing failed
     *
     * @param trace     the Context Trace information for the message association
     * @param throwable the Context Trace information for the message association
     */
    public static void ConsumerMsgFailure(String trace, Throwable throwable) {

    }


}
