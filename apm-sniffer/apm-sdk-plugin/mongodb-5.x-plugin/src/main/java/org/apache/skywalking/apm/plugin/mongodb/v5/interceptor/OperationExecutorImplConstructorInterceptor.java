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

package org.apache.skywalking.apm.plugin.mongodb.v5.interceptor;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

/**
 * Intercept {@code MongoClusterImpl$OperationExecutorImpl} constructor.
 * As a non-static inner class, the compiled constructor has the enclosing
 * {@code MongoClusterImpl} instance as a synthetic first argument (arg index 0).
 *
 * Note: This interceptor fires during MongoClusterImpl's constructor, before
 * MongoClusterImpl.onConstruct() sets the remotePeer. So the dynamic field
 * on the enclosing instance is not yet set. The primary peer propagation
 * happens in MongoClusterImplConstructorInterceptor.onConstruct() which
 * calls getOperationExecutor() after the constructor completes.
 *
 * This interceptor serves as a secondary path for OperationExecutorImpl
 * instances created later (e.g., via withTimeoutSettings()).
 */
public class OperationExecutorImplConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        for (Object arg : allArguments) {
            if (arg instanceof EnhancedInstance) {
                EnhancedInstance enclosingInstance = (EnhancedInstance) arg;
                String remotePeer = (String) enclosingInstance.getSkyWalkingDynamicField();
                if (remotePeer != null && !remotePeer.isEmpty()) {
                    objInst.setSkyWalkingDynamicField(remotePeer);
                    return;
                }
            }
        }
    }
}
