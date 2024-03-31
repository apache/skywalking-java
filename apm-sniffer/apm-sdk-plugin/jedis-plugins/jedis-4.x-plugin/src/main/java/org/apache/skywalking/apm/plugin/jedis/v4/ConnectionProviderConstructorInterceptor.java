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

package org.apache.skywalking.apm.plugin.jedis.v4;

import java.util.Collection;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.util.StringUtil;
import redis.clients.jedis.HostAndPort;

public class ConnectionProviderConstructorInterceptor implements InstanceConstructorInterceptor {
    @Override
    public void onConstruct(final EnhancedInstance objInst, final Object[] allArguments) throws Throwable {
        if (objInst.getSkyWalkingDynamicField() != null) {
            return;
        }
        Object arg = allArguments[0];
        if (arg instanceof Collection) {
            Collection<?> collection = (Collection<?>) arg;
            final String[] array = collection.stream().map(Object::toString).toArray(String[]::new);
            objInst.setSkyWalkingDynamicField(StringUtil.join(',', array));
        }
        if (arg instanceof HostAndPort) {
            objInst.setSkyWalkingDynamicField(arg.toString());
        }
    }

}
