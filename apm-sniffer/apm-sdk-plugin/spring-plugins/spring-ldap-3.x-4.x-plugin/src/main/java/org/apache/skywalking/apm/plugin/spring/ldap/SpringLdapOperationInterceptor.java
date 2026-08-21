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

package org.apache.skywalking.apm.plugin.spring.ldap;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.InstanceMethodsAroundInterceptorV2;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

public class SpringLdapOperationInterceptor implements InstanceMethodsAroundInterceptorV2 {

    private static final String AUTHENTICATE_OPERATION = "authenticate";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInvocationContext context) throws Throwable {
        Object dynamicField = objInst.getSkyWalkingDynamicField();
        if (!(dynamicField instanceof SpringLdapEnhanceInfo)) {
            return;
        }

        SpringLdapEnhanceInfo info = (SpringLdapEnhanceInfo) dynamicField;
        // Nested remote calls reuse the active exit span; skip so LDAP tags are not overwritten.
        if (ContextManager.isActive() && ContextManager.activeSpan().isExit()) {
            return;
        }

        String operation = info.getOperation() == null ? method.getName() : info.getOperation();
        AbstractSpan span = ContextManager.createExitSpan(
            SpringLdapConstants.OPERATION_PREFIX + operation, info.getPeer());
        try {
            span.setComponent(ComponentsDefine.SPRING_LDAP);
            SpanLayer.asDB(span);
            Tags.DB_TYPE.set(span, SpringLdapConstants.DB_TYPE);
            span.tag(SpringLdapConstants.LDAP_OPERATION, operation);
            context.setContext(new InvocationState(span, operation));
        } catch (Throwable initializationFailure) {
            // The V2 bridge continues the application even when beforeMethod fails.
            try {
                ContextManager.stopSpan(span);
            } catch (Throwable stopFailure) {
                initializationFailure.addSuppressed(stopFailure);
            }
            throw initializationFailure;
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret, MethodInvocationContext context) {
        InvocationState state = invocationState(context);
        if (state == null) {
            return ret;
        }
        try {
            if (failedPrimitiveBooleanAuthenticate(method, state.operation, ret)) {
                state.span.errorOccurred();
            }
            return ret;
        } finally {
            ContextManager.stopSpan(state.span);
        }
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t, MethodInvocationContext context) {
        InvocationState state = invocationState(context);
        if (state == null) {
            return;
        }
        // Default to a privacy-safe error flag. Exception messages/stacks can contain DNs,
        // filters, or credentials and are collected only when explicitly enabled.
        state.span.errorOccurred();
        if (SpringLdapPluginConfig.Plugin.SpringLDAP.COLLECT_EXCEPTION_DETAILS) {
            state.span.log(t);
        }
    }

    /**
     * Only primitive {@code boolean authenticate(...)} methods report invalid credentials as
     * {@code false}. Mapper overloads may return {@link Boolean#FALSE} as a successful mapped value.
     */
    private static boolean failedPrimitiveBooleanAuthenticate(Method method, String operation, Object ret) {
        return AUTHENTICATE_OPERATION.equals(operation)
            && method != null
            && method.getReturnType() == boolean.class
            && Boolean.FALSE.equals(ret);
    }

    private static InvocationState invocationState(MethodInvocationContext context) {
        Object state = context.getContext();
        return state instanceof InvocationState ? (InvocationState) state : null;
    }

    private static final class InvocationState {

        private final AbstractSpan span;

        private final String operation;

        private InvocationState(AbstractSpan span, String operation) {
            this.span = span;
            this.operation = operation;
        }
    }
}
