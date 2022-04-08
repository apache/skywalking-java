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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.shenyu.common.dto.PluginData;
import org.apache.shenyu.common.dto.RuleData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.plugin.base.cache.BaseDataCache;
import org.apache.shenyu.sync.data.api.PluginDataSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
        String pluginData = "{\"request\":{\"id\":\"20\",\"name\":\"request\",\"config\":null,"
                + "\"role\":\"HttpProcess\",\"enabled\":false,\"sort\":120},\"jwt\":{\"id\":\"19\",\"name\":\"jwt\","
                + "\"config\":\"{\\\"secretKey\\\":\\\"key\\\"}\",\"role\":\"Authentication\",\"enabled\":false,"
                + "\"sort\":30},\"paramMapping\":{\"id\":\"22\",\"name\":\"paramMapping\","
                + "\"config\":\"{\\\"ruleHandlePageType\\\":\\\"custom\\\"}\",\"role\":\"HttpProcess\","
                + "\"enabled\":false,\"sort\":70},\"modifyResponse\":{\"id\":\"23\",\"name\":\"modifyResponse\","
                + "\"config\":\"{\\\"ruleHandlePageType\\\":\\\"custom\\\"}\",\"role\":\"HttpProcess\","
                + "\"enabled\":false,\"sort\":220},\"sign\":{\"id\":\"1\",\"name\":\"sign\",\"config\":null,"
                + "\"role\":\"Authentication\",\"enabled\":false,\"sort\":20},\"dubbo\":{\"id\":\"6\","
                + "\"name\":\"dubbo\",\"config\":\"{\\\"register\\\":\\\"zookeeper://localhost:2181\\\","
                + "\\\"multiSelectorHandle\\\":\\\"1\\\",\\\"threadpool\\\":\\\"cached\\\",\\\"corethreads\\\":0,"
                + "\\\"threads\\\":2147483647,\\\"queues\\\":0}\",\"role\":\"Proxy\",\"enabled\":false,\"sort\":310},"
                + "\"motan\":{\"id\":\"17\",\"name\":\"motan\",\"config\":\"{\\\"register\\\":\\\"127.0.0"
                + ".1:2181\\\"}\",\"role\":\"Proxy\",\"enabled\":false,\"sort\":310},\"oauth2\":{\"id\":\"21\","
                + "\"name\":\"oauth2\",\"config\":null,\"role\":\"Authentication\",\"enabled\":false,\"sort\":40},"
                + "\"rateLimiter\":{\"id\":\"4\",\"name\":\"rateLimiter\","
                + "\"config\":\"{\\\"master\\\":\\\"mymaster\\\",\\\"mode\\\":\\\"standalone\\\",\\\"url\\\":\\\"192"
                + ".168.1.1:6379\\\",\\\"password\\\":\\\"abc\\\"}\",\"role\":\"FaultTolerance\",\"enabled\":false,"
                + "\"sort\":60},\"websocket\":{\"id\":\"26\",\"name\":\"websocket\","
                + "\"config\":\"{\\\"multiSelectorHandle\\\":\\\"1\\\"}\",\"role\":\"Proxy\",\"enabled\":true,"
                + "\"sort\":200},\"mqtt\":{\"id\":\"28\",\"name\":\"mqtt\",\"config\":\"{\\\"port\\\": 9500,"
                + "\\\"bossGroupThreadCount\\\": 1,\\\"maxPayloadSize\\\": 65536,\\\"workerGroupThreadCount\\\": 12,"
                + "\\\"userName\\\": \\\"shenyu\\\",\\\"password\\\": \\\"shenyu\\\",\\\"isEncryptPassword\\\": "
                + "false,\\\"encryptMode\\\": \\\"\\\",\\\"leakDetectorLevel\\\": \\\"DISABLED\\\"}\","
                + "\"role\":\"Proxy\",\"enabled\":false,\"sort\":125},\"tars\":{\"id\":\"13\",\"name\":\"tars "
                + "tested\",\"config\":\"{\\\"multiSelectorHandle\\\":\\\"1\\\",\\\"multiRuleHandle\\\":\\\"0\\\"}\","
                + "\"role\":\"Proxy\",\"enabled\":false,\"sort\":310},\"cryptorRequest\":{\"id\":\"24\","
                + "\"name\":\"cryptorRequest\",\"config\":null,\"role\":\"Cryptor\",\"enabled\":true,\"sort\":100},"
                + "\"divide\":{\"id\":\"5\",\"name\":\"divide\",\"config\":\"{\\\"multiSelectorHandle\\\":\\\"1\\\","
                + "\\\"multiRuleHandle\\\":\\\"0\\\"}\",\"role\":\"Proxy\",\"enabled\":true,\"sort\":200},"
                + "\"waf\":{\"id\":\"2\",\"name\":\"waf\",\"config\":\"{\\\"model\\\":\\\"black\\\"}\","
                + "\"role\":\"Authentication\",\"enabled\":false,\"sort\":50},\"redirect\":{\"id\":\"16\","
                + "\"name\":\"redirect\",\"config\":null,\"role\":\"HttpProcess\",\"enabled\":false,\"sort\":110},"
                + "\"sentinel\":{\"id\":\"10\",\"name\":\"sentinel\",\"config\":null,\"role\":\"FaultTolerance\","
                + "\"enabled\":false,\"sort\":140},\"hystrix\":{\"id\":\"9\",\"name\":\"hystrix\",\"config\":null,"
                + "\"role\":\"FaultTolerance\",\"enabled\":false,\"sort\":130},\"sofa\":{\"id\":\"11\","
                + "\"name\":\"sofa\",\"config\":\"{\\\"protocol\\\":\\\"zookeeper\\\",\\\"register\\\":\\\"127.0.0"
                + ".1:2181\\\"}\",\"role\":\"Proxy\",\"enabled\":false,\"sort\":310},\"cache\":{\"id\":\"30\","
                + "\"name\":\"cache\",\"config\":\"{\\\"cacheType\\\":\\\"memory\\\"}\",\"role\":\"Cache\","
                + "\"enabled\":false,\"sort\":10},\"contextPath\":{\"id\":\"14\",\"name\":\"contextPath\","
                + "\"config\":null,\"role\":\"HttpProcess\",\"enabled\":true,\"sort\":80},"
                + "\"generalContext\":{\"id\":\"27\",\"name\":\"generalContext\",\"config\":null,\"role\":\"Common\","
                + "\"enabled\":true,\"sort\":125},\"rewrite\":{\"id\":\"3\",\"name\":\"rewrite\",\"config\":null,"
                + "\"role\":\"HttpProcess\",\"enabled\":false,\"sort\":90},\"springCloud\":{\"id\":\"8\","
                + "\"name\":\"springCloud\",\"config\":null,\"role\":\"Proxy\",\"enabled\":false,\"sort\":200},"
                + "\"grpc\":{\"id\":\"15\",\"name\":\"grpc\",\"config\":\"{\\\"multiSelectorHandle\\\":\\\"1\\\","
                + "\\\"multiRuleHandle\\\":\\\"0\\\",\\\"threadpool\\\":\\\"cached\\\"}\",\"role\":\"Proxy\","
                + "\"enabled\":false,\"sort\":310},\"resilience4j\":{\"id\":\"12\",\"name\":\"resilience4j\","
                + "\"config\":null,\"role\":\"FaultTolerance\",\"enabled\":false,\"sort\":310},"
                + "\"logging\":{\"id\":\"18\",\"name\":\"logging\",\"config\":null,\"role\":\"Logging\","
                + "\"enabled\":false,\"sort\":160},\"cryptorResponse\":{\"id\":\"25\",\"name\":\"cryptorResponse\","
                + "\"config\":null,\"role\":\"Cryptor\",\"enabled\":true,\"sort\":410},"
                + "\"loggingRocketMQ\":{\"id\":\"29\",\"name\":\"loggingRocketMQ\","
                + "\"config\":\"{\\\"topic\\\":\\\"shenyu-access-logging\\\", \\\"namesrvAddr\\\": "
                + "\\\"localhost:9876\\\",\\\"producerGroup\\\":\\\"shenyu-plugin-logging-rocketmq\\\"}\","
                + "\"role\":\"Logging\",\"enabled\":false,\"sort\":170}}";

        String selectorData = "{\"contextPath\":[{\"id\":\"1512230008264994816\",\"pluginId\":\"14\","
                + "\"pluginName\":\"contextPath\",\"name\":\"/http\",\"matchMode\":0,\"type\":1,\"sort\":1,"
                + "\"enabled\":true,\"logged\":true,\"continued\":true,\"handle\":null,"
                + "\"conditionList\":[{\"paramType\":\"uri\",\"operator\":\"match\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/**\"}]}],\"divide\":[{\"id\":\"1512230008105611264\",\"pluginId\":\"5\","
                + "\"pluginName\":\"divide\",\"name\":\"/http\",\"matchMode\":0,\"type\":1,\"sort\":1,"
                + "\"enabled\":true,\"logged\":true,\"continued\":true,\"handle\":\"[{\\\"weight\\\":50,"
                + "\\\"warmup\\\":10,\\\"protocol\\\":\\\"http://\\\",\\\"upstreamHost\\\":\\\"localhost\\\","
                + "\\\"upstreamUrl\\\":\\\"localhost:8189\\\",\\\"status\\\":true,"
                + "\\\"timestamp\\\":1649378707380}]\",\"conditionList\":[{\"paramType\":\"uri\","
                + "\"operator\":\"match\",\"paramName\":\"/\",\"paramValue\":\"/http/**\"}]}]}\n";

        String ruleData = "{\"1512230008264994816\":[{\"id\":\"1512230008290160640\",\"name\":\"/http\","
                + "\"pluginName\":\"contextPath\",\"selectorId\":\"1512230008264994816\",\"matchMode\":0,\"sort\":1,"
                + "\"enabled\":true,\"loged\":true,\"handle\":\"{\\\"contextPath\\\":\\\"/http\\\"}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"match\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/**\"}]}],\"1512230008105611264\":[{\"id\":\"1512230008223051776\","
                + "\"name\":\"/http/order/path/**/name\",\"pluginName\":\"divide\","
                + "\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"match\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/order/path/**/name\"}]},{\"id\":\"1512230008755728384\","
                + "\"name\":\"/http/order\",\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\","
                + "\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/order\"}]},{\"id\":\"1512230009120632832\",\"name\":\"/http/order/save\","
                + "\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,"
                + "\"enabled\":true,\"loged\":true,\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\","
                + "\\\"retryStrategy\\\":\\\"current\\\",\\\"retry\\\":3,\\\"timeout\\\":3000,"
                + "\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/order/save\"}]},{\"id\":\"1512230009321959424\","
                + "\"name\":\"/http/order/path/**\",\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\","
                + "\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"match\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/order/path/**\"}]},{\"id\":\"1512230009363902464\","
                + "\"name\":\"/http/order/oauth2/test\",\"pluginName\":\"divide\","
                + "\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/order/oauth2/test\"}]},{\"id\":\"1512230009397456896\","
                + "\"name\":\"/http/test/**\",\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\","
                + "\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"match\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/test/**\"}]},{\"id\":\"1512230009481342976\","
                + "\"name\":\"/http/order/findById\",\"pluginName\":\"divide\","
                + "\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/order/findById\"}]},{\"id\":\"1512230009514897408\",\"name\":\"/http/\","
                + "\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,"
                + "\"enabled\":true,\"loged\":true,\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\","
                + "\\\"retryStrategy\\\":\\\"current\\\",\\\"retry\\\":3,\\\"timeout\\\":3000,"
                + "\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/\"}]},{\"id\":\"1512230009586200576\",\"name\":\"/http/post/hi\","
                + "\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,"
                + "\"enabled\":true,\"loged\":true,\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\","
                + "\\\"retryStrategy\\\":\\\"current\\\",\\\"retry\\\":3,\\\"timeout\\\":3000,"
                + "\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/post/hi\"}]},{\"id\":\"1512230009607172096\","
                + "\"name\":\"/http/shenyu/client/hi\",\"pluginName\":\"divide\","
                + "\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/shenyu/client/hi\"}]},{\"id\":\"1512230009632337920\","
                + "\"name\":\"/http/shenyu/client/timeout\",\"pluginName\":\"divide\","
                + "\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/shenyu/client/timeout\"}]},{\"id\":\"1512230009712029696\","
                + "\"name\":\"/http/shenyu/client/hello\",\"pluginName\":\"divide\","
                + "\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/shenyu/client/hello\"}]},{\"id\":\"1512230009728806912\","
                + "\"name\":\"/http/request/**\",\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\","
                + "\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"match\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/request/**\"}]},{\"id\":\"1512230009787527168\",\"name\":\"/http/hello\","
                + "\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,"
                + "\"enabled\":true,\"loged\":true,\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\","
                + "\\\"retryStrategy\\\":\\\"current\\\",\\\"retry\\\":3,\\\"timeout\\\":3000,"
                + "\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/hello\"}]},{\"id\":\"1512230009804304384\","
                + "\"name\":\"/http/shenyu/client/post/hi\",\"pluginName\":\"divide\","
                + "\"selectorId\":\"1512230008105611264\",\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/shenyu/client/post/hi\"}]},{\"id\":\"1512230009955299328\","
                + "\"name\":\"/http/hi\",\"pluginName\":\"divide\",\"selectorId\":\"1512230008105611264\","
                + "\"matchMode\":0,\"sort\":1,\"enabled\":true,\"loged\":true,"
                + "\"handle\":\"{\\\"loadBalance\\\":\\\"random\\\",\\\"retryStrategy\\\":\\\"current\\\","
                + "\\\"retry\\\":3,\\\"timeout\\\":3000,\\\"headerMaxSize\\\":10240,\\\"requestMaxSize\\\":102400}\","
                + "\"conditionDataList\":[{\"paramType\":\"uri\",\"operator\":\"=\",\"paramName\":\"/\","
                + "\"paramValue\":\"/http/hi\"}]}]}";

        Map<String, PluginData> pluginDataMap = GsonUtils.getInstance().toObjectMap(pluginData, PluginData.class);
        Map<String, List<SelectorData>> selectorDataMap = GsonUtils.getInstance().toObjectMapList(selectorData, SelectorData.class);
        Map<String, List<RuleData>> ruleDataMap = GsonUtils.getInstance().toObjectMapList(ruleData, RuleData.class);




        BaseDataCache dataCache = BaseDataCache.getInstance();
        pluginDataMap.values().forEach(subscriber::onSubscribe);
        selectorDataMap.values().stream().flatMap(Collection::stream).forEach(data -> {

            subscriber.onSelectorSubscribe(data);
            // dataCache.cacheSelectData(data);
            // DividePluginDataHandler.handlerSelector(data);

        });
        ruleDataMap.values().stream().flatMap(Collection::stream).forEach(subscriber::onRuleSubscribe);

        // String contextData = "{\"cached\":{\"1512230008264994816_/http\":{\"contextPath\":\"/http\"}}}";
        // Map<String, Object> contextMap = GsonUtils.getInstance().toObjectMap(contextData, Object.class);
        // contextMap.forEach((key, value) -> {
        //     String json = GsonUtils.getInstance().toJson(value);
        //     Map<String, ContextMappingRuleHandle> map =
        //             GsonUtils.getInstance().toObjectMap(json, ContextMappingRuleHandle.class);
        //     map.forEach((k, v) -> ContextPathPluginDataHandler.CACHED_HANDLE.get().cachedHandle(k, v));
        // });
        //
        // String ruleHandlerData = "{\"1512230008105611264_/http/order/path/**\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/order/path/**/name\":{\"loadBalance"
        //         + "\":\"random\",\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/post/hi\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/hi\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/test/**\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/shenyu/client/post/hi\":{\"loadBalance"
        //         + "\":\"random\",\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/order\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/order/oauth2/test\":{\"loadBalance"
        //         + "\":\"random\",\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/order/save\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/shenyu/client/hi\":{\"loadBalance"
        //         + "\":\"random\",\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/shenyu/client/hello\":{\"loadBalance"
        //         + "\":\"random\",\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/hello\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/request/**\":{\"loadBalance\":\"random\","
        //         + "\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/order/findById\":{\"loadBalance\":\"random"
        //         + "\",\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400},\"1512230008105611264_/http/shenyu/client/timeout\":{\"loadBalance"
        //         + "\":\"random\",\"retryStrategy\":\"current\",\"retry\":3,\"timeout\":3000,\"headerMaxSize\":10240,"
        //         + "\"requestMaxSize\":102400}}";
        //
        // Map<String, DivideRuleHandle> ruleHandleMap = GsonUtils.getInstance().toObjectMap(ruleHandlerData, DivideRuleHandle.class);
        // ruleHandleMap.forEach((k, v) -> DividePluginDataHandler.CACHED_HANDLE.get().cachedHandle(k, v));
        log.info("init router data finish.");
    }

}
