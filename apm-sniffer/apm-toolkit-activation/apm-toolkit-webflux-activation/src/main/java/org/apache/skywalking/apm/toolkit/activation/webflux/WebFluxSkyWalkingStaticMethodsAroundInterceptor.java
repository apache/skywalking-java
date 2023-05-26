package org.apache.skywalking.apm.toolkit.activation.webflux;

import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import java.util.Optional;


public abstract class WebFluxSkyWalkingStaticMethodsAroundInterceptor implements StaticMethodsAroundInterceptor {

    protected static EnhancedInstance getInstance(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof DefaultServerWebExchange && o instanceof EnhancedInstance) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getInstance(delegate);
        }
        return instance;
    }

    protected static ContextSnapshot getContextSnapshot(Object o) {
        return Optional.ofNullable(getInstance(o))
                .map(EnhancedInstance::getSkyWalkingDynamicField)
                .filter(ContextSnapshot.class::isInstance)
                .map(ContextSnapshot.class::cast)
                .orElse(null);
    }
}
