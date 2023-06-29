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
 */

package org.apache.skywalking.apm.plugin.aerospike;

import com.aerospike.client.Host;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.util.StringUtil;

import java.util.ArrayList;

public class AerospikeClientConstructorInterceptor implements InstanceConstructorInterceptor {
    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        String peer = "";
        if (allArguments.length >= 1 && allArguments[0] instanceof String) {
            peer = allArguments[0] + ":" + allArguments[1];
        } else if (allArguments.length >= 2 && allArguments[1] instanceof String) {
            peer = allArguments[1] + ":" + allArguments[2];
        } else if (allArguments.length >= 2 && allArguments[1] instanceof Host) {
            Host host = (Host)  allArguments[1];
            peer = host.name + ":" + host.port;
        } else if (allArguments.length >= 2 && allArguments[1] instanceof Host[]) {
            Host[] hosts = (Host[])  allArguments[1];
            ArrayList<String> names = new ArrayList<String>(hosts.length);
            for (Host host: hosts) {
                names.add(host.name + ":" + host.port);
            }
            peer = StringUtil.join(';', names.toArray(new String[0]));
        }
        objInst.setSkyWalkingDynamicField(peer);
    }
}