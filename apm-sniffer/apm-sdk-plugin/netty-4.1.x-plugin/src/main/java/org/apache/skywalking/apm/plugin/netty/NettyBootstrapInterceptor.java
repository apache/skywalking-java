package org.apache.skywalking.apm.plugin.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

public class NettyBootstrapInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        InetSocketAddress remoteAddress = (InetSocketAddress) allArguments[0];
        AbstractSpan span = ContextManager.createExitSpan("Netty/Client/connect", remoteAddress.getAddress().toString());
        span.setLayer(SpanLayer.HTTP);
        span.setComponent(ComponentsDefine.NETTY_CLIENT);
        span.asyncFinish();
        objInst.setSkyWalkingDynamicField(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        ChannelFuture promise = (ChannelPromise) ret;
        promise.addListener(f -> {
            AbstractSpan span = (AbstractSpan) objInst.getSkyWalkingDynamicField();
            if (f.isSuccess()) {
                span.asyncFinish();
            } else {
                span.errorOccurred();
                span.asyncFinish();
            }
        });
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
