//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.apache.skywalking.apm.plugin.jedis.v2;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import redis.clients.jedis.Client;

public class TransactionConstructorInterceptor implements InstanceConstructorInterceptor {
    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) throws Throwable {
        Client client = (Client) allArguments[0];

        objInst.setSkyWalkingDynamicField(client.getHost() + ":" + client.getPort());
    }
}
