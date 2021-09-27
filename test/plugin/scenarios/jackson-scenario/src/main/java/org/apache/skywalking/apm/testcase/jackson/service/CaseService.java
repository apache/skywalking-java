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

package org.apache.skywalking.apm.testcase.jackson.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.skywalking.apm.testcase.jackson.entity.CaseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class CaseService {

    public void mapperCase() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CaseEntity entity = new CaseEntity();
        mapper.writeValue(out, entity);

        Object jsonStr = mapper.writeValueAsString(entity);
        assert jsonStr instanceof String;
        Object jsonByte = mapper.writeValueAsBytes(entity);
        assert jsonByte instanceof byte[];

        String jsonString = "{\"key\":123, \"msg\":\"test\" }";
        Object jsonObj = mapper.readValue(jsonString, CaseEntity.class);
        assert jsonObj instanceof CaseEntity;
    }

    public void readerCase() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(CaseEntity.class);

        String jsonString = "{\"key\":123, \"msg\":\"test\" }";
        byte[] jsonBytes = jsonString.getBytes();

        Object jsonObj1 = reader.readValue(jsonString);
        assert jsonObj1 instanceof CaseEntity;
        Object jsonObj2 = reader.readValue(jsonBytes);
        assert jsonObj2 instanceof CaseEntity;
        Object jsonObj3 = reader.readValues(jsonString);
        assert jsonObj3 instanceof MappingIterator;
    }

    public void writerCase() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerFor(CaseEntity.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CaseEntity entity = new CaseEntity();
        writer.writeValue(out, entity);

        Object jsonStr = writer.writeValueAsString(entity);
        assert jsonStr instanceof String;
        Object jsonByte = writer.writeValueAsBytes(entity);
        assert jsonByte instanceof byte[];
    }
}
