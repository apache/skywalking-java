/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package test.apache.skywalking.apm.testcase.shenyu.gateway.init;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.shenyu.common.dto.MetaData;
import org.apache.shenyu.common.dto.PluginData;
import org.apache.shenyu.common.dto.RuleData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.plugin.global.cache.MetaDataCache;
import org.apache.shenyu.plugin.sofa.cache.ApplicationConfigCache;
import org.apache.shenyu.sync.data.api.PluginDataSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

/**
 * init gateway router runner.
 */
@Slf4j
@Component
public class InitRouterRunner implements CommandLineRunner {

    @Autowired
    private PluginDataSubscriber subscriber;

    @Override
    public void run(String... args) throws Exception {
        String pluginPath = "/shenyu-plugin.json";
        String selectorPath = "/shenyu-selector.json";
        String rulePath = "/shenyu-rule.json";

        try (
                InputStream pluginStream = getClass().getResourceAsStream(pluginPath);
                InputStream selectorStream = getClass().getResourceAsStream(selectorPath);
                InputStream ruleStream = getClass().getResourceAsStream(rulePath)) {

            Map<String, PluginData> pluginDataMap = JSON.parseObject(pluginStream,
                    new TypeReference<Map<String, PluginData>>() {
                    }.getType(), Feature.AllowComment);
            Map<String, List<SelectorData>> selectorDataMap = JSON.parseObject(selectorStream,
                    new TypeReference<Map<String, List<SelectorData>>>() {
                    }.getType(), Feature.AllowComment);
            Map<String, List<RuleData>> ruleDataMap = JSON.parseObject(ruleStream,
                    new TypeReference<Map<String, List<RuleData>>>() {
                    }.getType(), Feature.AllowComment);

            pluginDataMap.values().forEach(subscriber::onSubscribe);
            selectorDataMap.values().stream().flatMap(Collection::stream)
                    .forEach(data -> subscriber.onSelectorSubscribe(data));
            ruleDataMap.values().stream().flatMap(Collection::stream).forEach(subscriber::onRuleSubscribe);

            initGrpc();
            log.info("init router data finish.");
        } catch (Exception e) {
            log.error("init router failed", e);
        }
    }

    private void initGrpc() throws Exception {
        MetaData metaData = new MetaData();
        metaData.setId("1");
        metaData.setAppName("sofa");
        metaData.setContextPath("/sofa");
        metaData.setPath("/sofa/hello");
        metaData.setRpcType("sofa");
        metaData.setServiceName("test.apache.skywalking.apm.testcase.shenyu.sofarpc.interfaces.SofaRpcDemoService");
        metaData.setMethodName("hello");
        metaData.setParameterTypes(null);
        metaData.setRpcExt("{\"loadbalance\":\"hash\",\"retries\":3,\"timeout\":-1}");
        metaData.setEnabled(true);
        MetaDataCache.getInstance().cache(metaData);

        Field cacheField = Arrays.stream(ApplicationConfigCache.class.getDeclaredFields())
                .filter(field -> field.getName().equals("cache"))
                .findFirst().get();

        cacheField.setAccessible(true);
        com.google.common.cache.Cache cache =
                (com.google.common.cache.Cache) cacheField.get(ApplicationConfigCache.getInstance());

        ConsumerConfig<GenericService> reference = new ConsumerConfig<>();
        reference.setGeneric(true);
        reference.setInterfaceId("test.apache.skywalking.apm.testcase.shenyu.sofarpc.interfaces.SofaRpcDemoService");
        reference.setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        reference.setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK);
        reference.setRepeatedReferLimit(-1);
        reference.setTimeout(5000);
        reference.setDirectUrl("bolt://127.0.0.1:12200");
        cache.put(metaData.getPath(), reference);

    }

}
