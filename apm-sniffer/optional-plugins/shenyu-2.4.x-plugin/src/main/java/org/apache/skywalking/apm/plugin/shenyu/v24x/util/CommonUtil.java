package org.apache.skywalking.apm.plugin.shenyu.v24x.util;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

/**
 * @author hutaishi@qq.com
 * @date 2022-04-17
 */
public class CommonUtil {

    public static EnhancedInstance getEnhancedServerWebExchange(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof DefaultServerWebExchange) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getEnhancedServerWebExchange(delegate);
        }
        return instance;
    }
}
