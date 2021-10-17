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

package org.apache.skywalking.apm.testcase.neo4j.controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.ClickHouseStatement;

@RestController
@RequestMapping("/case")
public class CaseController {

    private static final String SUCCESS = "Success";
    private static final String SQL = "SELECT * FROM clusters";
    @Resource
    private ClickHouseDataSource dataSource;

    @RequestMapping("/clickhouse-scenario")
    @ResponseBody
    public String testcase() throws Exception {
        try (ClickHouseConnection conn = dataSource.getConnection();
                ClickHouseStatement stmt = conn.createStatement();
                ResultSet ignored = stmt.executeQuery(SQL)) {
            conn.isValid(3);
        }

        try (final ClickHouseConnection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement(SQL);
                final ResultSet ignored = preparedStatement.executeQuery()) {
            connection.isValid(3);
        }
        return SUCCESS;
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() throws Exception {
        return SUCCESS;
    }

}
