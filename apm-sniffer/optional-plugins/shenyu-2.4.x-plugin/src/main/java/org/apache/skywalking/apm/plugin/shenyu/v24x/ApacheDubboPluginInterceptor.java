package org.apache.skywalking.apm.plugin.shenyu.v24x;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.shenyu.v24x.util.CommonUtil;
import org.springframework.web.server.ServerWebExchange;

import java.lang.reflect.Method;

/**
 * @author hutaishi@qq.com
 * @date 2022-04-17
 */
public class ApacheDubboPluginInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        ServerWebExchange exchange = (ServerWebExchange) allArguments[0];
        EnhancedInstance instance = CommonUtil.getEnhancedServerWebExchange(allArguments[0]);

        RpcContext.getContext().set("my", instance.getSkyWalkingDynamicField());

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
