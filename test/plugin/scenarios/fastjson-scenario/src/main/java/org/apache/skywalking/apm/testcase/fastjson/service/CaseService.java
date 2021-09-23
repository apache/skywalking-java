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

package org.apache.skywalking.apm.testcase.fastjson.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.util.IOUtils;
import org.apache.skywalking.apm.testcase.fastjson.entity.CaseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class CaseService {

    public void parseCase() {
        String jsonStr = "{\"key\":123}";
        Object jsonObj = JSON.parse(jsonStr, ParserConfig.getGlobalInstance(), JSON.DEFAULT_GENERATE_FEATURE);
        assert jsonObj instanceof JSONObject;
    }

    public void parseArrayCase() {
        String jsonStr = "[{\"key\":123},{\"key\":456}]";
        Object jsonObj = JSON.parseArray(jsonStr);
        assert jsonObj instanceof JSONArray;
    }

    public void parseObjectCase() {
        String jsonStr = "{\"key\":123,\"msg\":\"test\"}";
        Object jsonObj = JSON.parseObject(jsonStr, CaseEntity.class, ParserConfig.global, null, JSON.DEFAULT_PARSER_FEATURE, new Feature[0]);
        assert jsonObj instanceof CaseEntity;
    }

    public void toJavaObjectCase() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", 123);
        jsonObject.put("msg", "test");
        Object jsonObj = JSON.toJavaObject(jsonObject, CaseEntity.class);
        assert jsonObj instanceof CaseEntity;
    }

    public void toJsonCase() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", 123);
        jsonObject.put("msg", "test");
        Object jsonObj = JSON.toJSON(jsonObject, SerializeConfig.globalInstance);
        assert jsonObj instanceof JSONObject;
    }

    public void toJsonBytesCase() {
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setKey(123);
        caseEntity.setMsg("test");
        Object jsonObj = JSON.toJSONBytes(IOUtils.UTF8, caseEntity, SerializeConfig.globalInstance, new SerializeFilter[0], null, JSON.DEFAULT_GENERATE_FEATURE);
        assert jsonObj instanceof byte[];
    }

    public void toJsonStringCase() {
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setKey(123);
        caseEntity.setMsg("test");
        Object jsonObj = JSON.toJSONString(caseEntity, SerializeConfig.globalInstance, new SerializeFilter[0], null, JSON.DEFAULT_GENERATE_FEATURE);
        assert jsonObj instanceof String;
    }

    public void writeJsonStringCase() throws IOException {
        ByteArrayOutputStream outnew = new ByteArrayOutputStream();
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setKey(123);
        caseEntity.setMsg("test");
        Object jsonObj = JSON.writeJSONString(outnew, IOUtils.UTF8, caseEntity, SerializeConfig.globalInstance, new SerializeFilter[0], null, JSON.DEFAULT_GENERATE_FEATURE);
        assert jsonObj instanceof Integer;
    }
}
