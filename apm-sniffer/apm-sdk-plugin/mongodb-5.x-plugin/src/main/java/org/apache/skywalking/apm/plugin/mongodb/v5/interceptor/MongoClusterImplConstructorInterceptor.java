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

import com.mongodb.internal.connection.Cluster;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.mongodb.v4.support.MongoRemotePeerHelper;

import java.lang.reflect.Method;

/**
 * Intercept {@code MongoClusterImpl} constructor and {@code getOperationExecutor()}.
 * <p>
 * Constructor: extract remotePeer from Cluster (arg[1]) and store in dynamic field.
 * getOperationExecutor(): pass remotePeer to the returned OperationExecutor.
 */
public class MongoClusterImplConstructorInterceptor
    implements InstanceConstructorInterceptor, InstanceMethodsAroundInterceptor {

    private static final ILog LOGGER = LogManager.getLogger(MongoClusterImplConstructorInterceptor.class);

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        Cluster cluster = (Cluster) allArguments[1];
        String remotePeer = MongoRemotePeerHelper.getRemotePeer(cluster);
        objInst.setSkyWalkingDynamicField(remotePeer);

        // The OperationExecutorImpl is created INSIDE this constructor (before onConstruct fires),
        // so its constructor interceptor couldn't read the peer yet. Set it now.
        // MongoClusterImpl is package-private, access getOperationExecutor via reflection.
        try {
            java.lang.reflect.Method getExecutor = objInst.getClass().getMethod("getOperationExecutor");
            getExecutor.setAccessible(true);
            Object executor = getExecutor.invoke(objInst);
            if (executor instanceof EnhancedInstance) {
                ((EnhancedInstance) executor).setSkyWalkingDynamicField(remotePeer);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to set remotePeer on OperationExecutor", e);
        }
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) {
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) {
        if (ret instanceof EnhancedInstance) {
            EnhancedInstance retInstance = (EnhancedInstance) ret;
            String remotePeer = (String) objInst.getSkyWalkingDynamicField();
            if (LOGGER.isDebugEnable()) {
                LOGGER.debug("Mark OperationExecutor remotePeer: {}", remotePeer);
            }
            retInstance.setSkyWalkingDynamicField(remotePeer);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
    }
}
