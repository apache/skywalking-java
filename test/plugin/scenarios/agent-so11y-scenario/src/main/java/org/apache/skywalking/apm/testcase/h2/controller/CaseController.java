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

package org.apache.skywalking.apm.testcase.h2.controller;

import java.net.URL;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/case")
@Log4j2
public class CaseController {

    private static final String SUCCESS = "Success";

    private static final String CREATE_TABLE_SQL = "CREATE TABLE test_007(\n" +
        "id VARCHAR(1) PRIMARY KEY, \n" +
        "value VARCHAR(1) NOT NULL)";
    private static final String INSERT_DATA_SQL = "INSERT INTO test_007(id, value) VALUES(?,?)";
    private static final String DROP_TABLE_SQL = "DROP table test_007";

    @RequestMapping("/healthCheck.html")
    @ResponseBody
    public String healthCheck() {
        // your codes
        return SUCCESS;
    }

    @RequestMapping("/agent-so11y-scenario")
    @ResponseBody
    public String testcase() throws Exception {
        try (SQLExecutor sqlExecute = new SQLExecutor()) {
            sqlExecute.createTable(CREATE_TABLE_SQL);
            sqlExecute.insertData(INSERT_DATA_SQL, "1", "1");
            sqlExecute.dropTable(DROP_TABLE_SQL);

            // test bootstrap plugin & ignore context counter
            new URL("http://localhost:8080/agent-so11y-scenario/case/ignore.html").getContent();
            new URL("http://localhost:8080/agent-so11y-scenario/case/propagated").getContent();
        } catch (Exception e) {
            log.error("Failed to execute sql.", e);
            throw e;
        }
        return SUCCESS;
    }

    @RequestMapping("/ignore.html")
    @ResponseBody
    public String ignorePath() {
        return SUCCESS;
    }

    @RequestMapping("/propagated")
    @ResponseBody
    public String propagated() {
        return SUCCESS;
    }
}
