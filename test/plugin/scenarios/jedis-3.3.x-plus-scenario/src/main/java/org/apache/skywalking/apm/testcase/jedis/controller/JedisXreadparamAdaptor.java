/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.testcase.jedis.controller;

import redis.clients.jedis.StreamEntryID;
import java.lang.reflect.Method;
import java.util.Map;

public class JedisXreadparamAdaptor {

    //  The stream apis have changed from 3.6.0
    public static Object callXReadMethod(Object target, int count, Map<String, StreamEntryID> map) {
        try {
            Class<?> xReadParamsCls = Class.forName("redis.clients.jedis.params.XReadParams");
            Object xReadParams = xReadParamsCls.getConstructor().newInstance();
            xReadParamsCls.getDeclaredMethod("count", Integer.TYPE).invoke(xReadParams, count);
            Method xread = target.getClass().getDeclaredMethod("xread", xReadParamsCls, Map.class);
            return xread.invoke(target, xReadParams, map);
        } catch (Exception e) {
            Map.Entry<String, StreamEntryID>[] entries = new Map.Entry[map.size()];
            int i = 0;
            for (Map.Entry<String, StreamEntryID> stringStreamEntryIDEntry : map.entrySet()) {
                entries[i++] = stringStreamEntryIDEntry;
            }
            try {
                return target.getClass().getDeclaredMethod("xread", Integer.TYPE, Long.TYPE, Map.Entry[].class).invoke(target, count, 0, entries);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
