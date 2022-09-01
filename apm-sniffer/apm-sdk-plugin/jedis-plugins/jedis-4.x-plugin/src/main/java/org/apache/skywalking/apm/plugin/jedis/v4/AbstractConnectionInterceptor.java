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

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.commands.ProtocolCommand;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Optional;

public abstract class AbstractConnectionInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String UNKNOWN = "UNKNOWN";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Iterator<Rawable> iterator = getCommands(allArguments);
        ProtocolCommand protocolCommand = null;
        if (iterator.hasNext()) {
            protocolCommand = (ProtocolCommand) iterator.next();
        }
        String cmd = protocolCommand == null ? UNKNOWN : protocolCommand.toString();
        String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        AbstractSpan span = ContextManager.createExitSpan("Jedis/" + cmd, peer);
        String params = readParamIfNecessary(iterator).map(arg -> cmd + " " + arg).orElse(cmd);
        Tags.DB_STATEMENT.set(span, params);
        span.setComponent(ComponentsDefine.JEDIS);
        Tags.DB_TYPE.set(span, "Redis");
        SpanLayer.asCache(span);
    }

    private Optional<String> readParamIfNecessary(Iterator<Rawable> iterator) {
        if (JedisPluginConfig.Plugin.Jedis.TRACE_REDIS_PARAMETERS && iterator.hasNext()) {
            Rawable rawable = iterator.next();
            if (rawable instanceof RawableFactory.RawString) {
                String cut = StringUtil.cut(new String(rawable.getRaw()), JedisPluginConfig.Plugin.Jedis.REDIS_PARAMETER_MAX_LENGTH);
                return Optional.of(cut);
            }
        }
        return Optional.empty();
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan().log(t).errorOccurred();
        ContextManager.stopSpan(span);
    }

    protected abstract Iterator<Rawable> getCommands(Object[] allArguments);
}
