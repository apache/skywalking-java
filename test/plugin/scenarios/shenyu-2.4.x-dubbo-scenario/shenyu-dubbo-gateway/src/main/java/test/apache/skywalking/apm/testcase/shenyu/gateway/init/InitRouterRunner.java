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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.shenyu.common.dto.MetaData;
import org.apache.shenyu.common.dto.PluginData;
import org.apache.shenyu.common.dto.RuleData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.common.dto.convert.plugin.DubboRegisterConfig;
import org.apache.shenyu.plugin.apache.dubbo.cache.ApacheDubboConfigCache;
import org.apache.shenyu.plugin.global.cache.MetaDataCache;
import org.apache.shenyu.sync.data.api.PluginDataSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
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

            initDubbo();
            log.info("init router data finish.");
        } catch (Exception e) {
            log.error("init router failed", e);
        }
    }

    private void initDubbo() {
        DubboRegisterConfig config = new DubboRegisterConfig();
        config.setProtocol("dubbo");
        config.setRegister("N/A");
        ApacheDubboConfigCache.getInstance().init(config);

        MetaData metaData = new MetaData();
        metaData.setId("1");
        metaData.setAppName("demo");
        metaData.setContextPath("/dubbo");
        metaData.setPath("/dubbo/findById");
        metaData.setRpcType("dubbo");
        metaData.setServiceName("test.apache.skywalking.apm.testcase.shenyu.http.services.OrderService");
        metaData.setMethodName("findById");
        metaData.setParameterTypes("java.lang.String");
        metaData.setRpcExt("{\"url\":\"dubbo://localhost:9999\",\"loadbalance\":\"random\",\"retries\":0,"
                + "\"timeout\":3000,\"sent\":false,\"cluster\":\"failover\"}");
        metaData.setEnabled(true);
        ApacheDubboConfigCache.getInstance().build(metaData);
        MetaDataCache.getInstance().cache(metaData);
    }

}
